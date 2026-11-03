package com.stayhub.room.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.room.dto.HousekeepingTaskCreateRequest;
import com.stayhub.room.dto.HousekeepingTaskResponse;
import com.stayhub.room.dto.HousekeepingTaskStatusChangeRequest;
import com.stayhub.room.dto.HousekeepingTaskUpdateRequest;
import com.stayhub.room.dto.RoomStatusChangeRequest;
import com.stayhub.room.entity.HousekeepingTask;
import com.stayhub.room.entity.HousekeepingTaskStatus;
import com.stayhub.room.entity.HousekeepingTaskType;
import com.stayhub.room.entity.RoomStatus;
import com.stayhub.room.repository.HousekeepingTaskRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 청소/점검 작업 스케줄 업무 로직.
 *
 * <p>컨트롤러에게 요청 DTO 를 받아 엔티티를 만들거나 고치고, 결과를 응답 DTO 로 바꿔 돌려줍니다.
 * 엔티티는 이 클래스 밖으로 나가지 않습니다.
 *
 * <p><b>트랜잭션 규칙</b> — 클래스에 {@code @Transactional(readOnly = true)}, 쓰기 메서드(등록·수정·상태 변경)에만 {@code @Transactional}.
 */
@Service
@Transactional(readOnly = true)
public class HousekeepingTaskService {

	/** 목록 정렬: task_id 오름차순(등록 순). */
	private static final Sort LIST_SORT = Sort.by(
			Sort.Order.asc("id")
	);

	private final HousekeepingTaskRepository housekeepingTaskRepository;
	private final RoomService roomService;

	public HousekeepingTaskService(HousekeepingTaskRepository housekeepingTaskRepository, RoomService roomService) {
		this.housekeepingTaskRepository = housekeepingTaskRepository;
		this.roomService = roomService;
	}

	/**
	 * 목록. 한 페이지 20건.
	 *
	 * @param page 0부터 시작
	 */
	public PageResponse<HousekeepingTaskResponse> getList(int page) {
		Pageable pageable = PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE, LIST_SORT);
		return PageResponse.from(housekeepingTaskRepository.findAll(pageable).map(HousekeepingTaskResponse::from));
	}

	/** 상세. 없는 항목이면 BusinessException. */
	public HousekeepingTaskResponse getDetail(Long id) {
		return HousekeepingTaskResponse.from(findOrThrow(id));
	}

	/** 등록. */
	@Transactional
	public HousekeepingTaskResponse create(HousekeepingTaskCreateRequest request) {
		HousekeepingTask housekeepingTask = HousekeepingTask.create(
				request.roomId(),
				request.taskType(),
				request.checkoutTime(),
				request.assignedStaffId(),
				request.isReassigned(),
				request.scheduledAt()
		);
		return HousekeepingTaskResponse.from(housekeepingTaskRepository.save(housekeepingTask));
	}

	/** 수정. 없는 항목이면 BusinessException. */
	@Transactional
	public HousekeepingTaskResponse update(Long id, HousekeepingTaskUpdateRequest request) {
		HousekeepingTask housekeepingTask = findOrThrow(id);
		housekeepingTask.update(
				request.roomId(),
				request.taskType(),
				request.checkoutTime(),
				request.assignedStaffId(),
				request.isReassigned(),
				request.scheduledAt()
		);
		// save() 를 부르지 않아도 트랜잭션이 끝날 때 바뀐 값이 자동으로 DB 에 반영됩니다. (변경 감지)
		// 다만 수정일시(updatedAt)는 반영되는 순간에 채워지므로, 응답에 새 값을 담으려고 먼저 반영합니다.
		housekeepingTaskRepository.flush();
		return HousekeepingTaskResponse.from(housekeepingTask);
	}

	/**
	 * 작업 상태 변경 (ROM-003). 순서는 SCHEDULED → IN_PROGRESS → COMPLETED 만 허용합니다.
	 *
	 * <p>청소 작업이면 객실 상태도 {@link RoomService#changeStatus} 로 함께 바꿉니다.
	 * 청소 시작은 CLEANING_NEEDED → CLEANING, 청소 완료는 CLEANING → VACANT 입니다. 점검 작업은 객실 상태를 바꾸지 않습니다.
	 *
	 * <p>작업과 객실은 한 트랜잭션이라 둘 다 바뀌거나 둘 다 안 바뀝니다. 객실 상태가 기대한 값과 달라 반려되면
	 * 이 메서드가 전체를 되돌리므로, RoomService 가 남기려던 충돌 이력도 남지 않습니다.
	 *
	 * @param changedBy 변경 담당자 ID. 컨트롤러가 로그인 세션에서 꺼내 넘깁니다.
	 */
	@Transactional
	public HousekeepingTaskResponse changeStatus(Long id, HousekeepingTaskStatusChangeRequest request, Long changedBy) {
		HousekeepingTask housekeepingTask = findOrThrow(id);
		HousekeepingTaskStatus newStatus = request.newStatus();
		validateTransition(housekeepingTask.getStatus(), newStatus);

		if (housekeepingTask.getTaskType() == HousekeepingTaskType.CLEANING) {
			changeRoomStatus(housekeepingTask.getRoomId(), newStatus, changedBy);
		}
		housekeepingTask.changeStatus(newStatus);
		// 수정일시(updatedAt)를 응답에 담으려고 먼저 반영합니다. (update 와 같은 이유)
		housekeepingTaskRepository.flush();
		return HousekeepingTaskResponse.from(housekeepingTask);
	}

	/** 한 단계 앞으로 가는 변경만 통과시킵니다. */
	private void validateTransition(HousekeepingTaskStatus current, HousekeepingTaskStatus next) {
		boolean allowed = (current == HousekeepingTaskStatus.SCHEDULED && next == HousekeepingTaskStatus.IN_PROGRESS)
				|| (current == HousekeepingTaskStatus.IN_PROGRESS && next == HousekeepingTaskStatus.COMPLETED);
		if (!allowed) {
			throw new BusinessException("작업 상태는 배정됨 → 진행 중 → 완료 순서로만 바꿀 수 있습니다.");
		}
	}

	/** 작업 상태에 맞춰 객실 상태를 바꿉니다. */
	private void changeRoomStatus(Long roomId, HousekeepingTaskStatus newStatus, Long changedBy) {
		if (newStatus == HousekeepingTaskStatus.IN_PROGRESS) {
			checkRoomStatus(roomId, RoomStatus.CLEANING_NEEDED, "객실이 청소 필요 상태가 아니라 청소를 시작할 수 없습니다.");
			roomService.changeStatus(roomId, new RoomStatusChangeRequest(
					RoomStatus.CLEANING_NEEDED, RoomStatus.CLEANING, "청소 시작"), changedBy);
		} else {
			checkRoomStatus(roomId, RoomStatus.CLEANING, "객실이 청소 중 상태가 아니라 청소를 완료할 수 없습니다.");
			roomService.changeStatus(roomId, new RoomStatusChangeRequest(
					RoomStatus.CLEANING, RoomStatus.VACANT, "청소 완료"), changedBy);
		}
	}

	/** 객실 상태가 맞지 않으면 실제 이유를 알려 줍니다. 확인 없이 부르면 "다른 담당자가 먼저…" 문구가 나갑니다. */
	private void checkRoomStatus(Long roomId, RoomStatus required, String message) {
		if (roomService.getDetail(roomId).status() != required) {
			throw new BusinessException(message);
		}
	}

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private HousekeepingTask findOrThrow(Long id) {
		return housekeepingTaskRepository.findById(id)
				.orElseThrow(() -> new BusinessException("청소/점검 작업을 찾을 수 없습니다."));
	}
}

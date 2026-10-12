package com.stayhub.room.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.common.PropertyScope;
import com.stayhub.room.dto.RoomCreateRequest;
import com.stayhub.room.dto.RoomResponse;
import com.stayhub.room.dto.RoomStatusChangeRequest;
import com.stayhub.room.dto.RoomUpdateRequest;
import com.stayhub.room.entity.Room;
import com.stayhub.room.entity.RoomStatus;
import com.stayhub.room.entity.RoomStatusLog;
import com.stayhub.room.repository.RoomRepository;
import com.stayhub.room.repository.RoomStatusLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 객실 업무 로직.
 *
 * <p>컨트롤러에게 요청 DTO 를 받아 엔티티를 만들거나 고치고, 결과를 응답 DTO 로 바꿔 돌려줍니다.
 * 엔티티는 이 클래스 밖으로 나가지 않습니다.
 *
 * <p><b>트랜잭션 규칙</b> — 클래스에 {@code @Transactional(readOnly = true)}, 쓰기 메서드(등록·수정·상태 변경)에만 {@code @Transactional}.
 *
 * <p><b>숙소 ID</b> — {@code property_id} 는 등록할 때만 {@link PropertyScope} 값을 넣고,
 * 조회 조건에는 넣지 않습니다(정책정의서 32행).
 */
@Service
@Transactional(readOnly = true)
public class RoomService {

	/** 목록 정렬: room_id 오름차순(등록 순). */
	private static final Sort LIST_SORT = Sort.by(
			Sort.Order.asc("id")
	);

	private static final String DUPLICATE_ROOM_NUMBER_MESSAGE = "이미 등록된 객실 번호입니다.";

	private final RoomRepository roomRepository;
	private final RoomStatusLogRepository roomStatusLogRepository;

	public RoomService(RoomRepository roomRepository, RoomStatusLogRepository roomStatusLogRepository) {
		this.roomRepository = roomRepository;
		this.roomStatusLogRepository = roomStatusLogRepository;
	}

	/**
	 * 목록. 한 페이지 20건.
	 *
	 * @param page 0부터 시작
	 */
	public PageResponse<RoomResponse> getList(int page) {
		Pageable pageable = PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE, LIST_SORT);
		return PageResponse.from(roomRepository.findAll(pageable).map(RoomResponse::from));
	}

	/** 상세. 없는 항목이면 BusinessException. */
	public RoomResponse getDetail(Long id) {
		return RoomResponse.from(findOrThrow(id));
	}

	/** 등록. property_id 는 요청에서 받지 않고 여기서 상수로 넣습니다. 같은 객실 번호가 있으면 BusinessException. */
	@Transactional
	public RoomResponse create(RoomCreateRequest request) {
		// room_number 는 UNIQUE 라서, 미리 확인하지 않으면 DB 오류(500)로 끝납니다.
		if (roomRepository.existsByRoomNumber(request.roomNumber())) {
			throw new BusinessException(DUPLICATE_ROOM_NUMBER_MESSAGE);
		}

		Room room = Room.create(
				PropertyScope.DEFAULT_PROPERTY_ID,
				request.roomNumber(),
				request.roomType(),
				request.basePrice(),
				request.maxOccupancy()
		);
		return RoomResponse.from(roomRepository.save(room));
	}

	/** 수정. 없는 항목이거나 다른 객실이 같은 객실 번호를 쓰고 있으면 BusinessException. */
	@Transactional
	public RoomResponse update(Long id, RoomUpdateRequest request) {
		Room room = findOrThrow(id);
		if (roomRepository.existsByRoomNumberAndIdNot(request.roomNumber(), id)) {
			throw new BusinessException(DUPLICATE_ROOM_NUMBER_MESSAGE);
		}

		room.update(
				request.roomNumber(),
				request.roomType(),
				request.basePrice(),
				request.maxOccupancy()
		);
		// save() 를 부르지 않아도 트랜잭션이 끝날 때 바뀐 값이 자동으로 DB 에 반영됩니다. (변경 감지)
		// 다만 수정일시(updatedAt)는 반영되는 순간에 채워지므로, 응답에 새 값을 담으려고 먼저 반영합니다.
		roomRepository.flush();
		return RoomResponse.from(room);
	}

	/**
	 * 객실 상태 변경 (ROM-001). 바꿀 때마다 room_status_logs 에 이력을 남깁니다.
	 *
	 * <p><b>동시 변경 충돌</b> — 요청의 previousStatus(화면이 보고 있던 상태)가 DB 의 실제 상태와 다르면
	 * 그 사이 다른 담당자가 먼저 바꾼 것입니다. 이때는 is_conflict=TRUE 이력만 남기고 상태는 바꾸지 않은 채 반려합니다.
	 *
	 * <p>{@code noRollbackFor} — 반려할 때 BusinessException 을 던지면 보통 트랜잭션 전체가 되돌려져
	 * 충돌 이력까지 사라집니다. 충돌 이력은 남겨야 하므로 이 예외로는 되돌리지 않게 합니다.
	 * 반려되는 경우 이 메서드가 DB 에 쓰는 것은 충돌 이력 한 줄뿐입니다.
	 */
	@Transactional(noRollbackFor = BusinessException.class)
	public RoomResponse changeStatus(Long id, RoomStatusChangeRequest request) {
		Room room = findOrThrow(id);
		RoomStatus currentStatus = room.getStatus();

		if (currentStatus != request.previousStatus()) {
			// 충돌 이력의 변경 전 상태는 화면이 본 값이 아니라 DB 의 실제 상태로 남깁니다.
			roomStatusLogRepository.save(RoomStatusLog.create(
					id, currentStatus, request.newStatus(), request.changeReason(), request.changedBy(), true));
			throw new BusinessException("다른 담당자가 먼저 객실 상태를 바꿨습니다. 새로고침 후 다시 시도해 주세요.");
		}
		if (currentStatus == request.newStatus()) {
			throw new BusinessException("이미 같은 상태입니다.");
		}

		room.changeStatus(request.newStatus(), request.changedBy());
		roomStatusLogRepository.save(RoomStatusLog.create(
				id, currentStatus, request.newStatus(), request.changeReason(), request.changedBy(), false));
		// 수정일시(updatedAt)를 응답에 담으려고 먼저 반영합니다. (update 와 같은 이유)
		roomRepository.flush();
		return RoomResponse.from(room);
	}

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private Room findOrThrow(Long id) {
		return roomRepository.findById(id)
				.orElseThrow(() -> new BusinessException("객실을 찾을 수 없습니다."));
	}
}

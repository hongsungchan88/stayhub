package com.stayhub.room.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.room.dto.RoomStatusLogCreateRequest;
import com.stayhub.room.dto.RoomStatusLogResponse;
import com.stayhub.room.dto.RoomStatusLogUpdateRequest;
import com.stayhub.room.entity.RoomStatusLog;
import com.stayhub.room.repository.RoomStatusLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 객실 상태 변경 이력 업무 로직.
 *
 * <p>컨트롤러에게 요청 DTO 를 받아 엔티티를 만들거나 고치고, 결과를 응답 DTO 로 바꿔 돌려줍니다.
 * 엔티티는 이 클래스 밖으로 나가지 않습니다.
 *
 * <p><b>트랜잭션 규칙</b> — 클래스에 {@code @Transactional(readOnly = true)}, 쓰기 메서드(등록·수정)에만 {@code @Transactional}.
 */
@Service
@Transactional(readOnly = true)
public class RoomStatusLogService {

	/** 목록 정렬: status_log_id 오름차순(등록 순). */
	private static final Sort LIST_SORT = Sort.by(
			Sort.Order.asc("id")
	);

	private final RoomStatusLogRepository roomStatusLogRepository;

	public RoomStatusLogService(RoomStatusLogRepository roomStatusLogRepository) {
		this.roomStatusLogRepository = roomStatusLogRepository;
	}

	/**
	 * 목록. 한 페이지 20건.
	 *
	 * @param page 0부터 시작
	 */
	public PageResponse<RoomStatusLogResponse> getList(int page) {
		Pageable pageable = PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE, LIST_SORT);
		return PageResponse.from(roomStatusLogRepository.findAll(pageable).map(RoomStatusLogResponse::from));
	}

	/** 상세. 없는 항목이면 BusinessException. */
	public RoomStatusLogResponse getDetail(Long id) {
		return RoomStatusLogResponse.from(findOrThrow(id));
	}

	/** 등록. */
	@Transactional
	public RoomStatusLogResponse create(RoomStatusLogCreateRequest request) {
		RoomStatusLog roomStatusLog = RoomStatusLog.create(
				request.roomId(),
				request.previousStatus(),
				request.newStatus(),
				request.changeReason(),
				request.changedBy(),
				request.isConflict()
		);
		return RoomStatusLogResponse.from(roomStatusLogRepository.save(roomStatusLog));
	}

	/** 수정. 없는 항목이면 BusinessException. */
	@Transactional
	public RoomStatusLogResponse update(Long id, RoomStatusLogUpdateRequest request) {
		RoomStatusLog roomStatusLog = findOrThrow(id);
		roomStatusLog.update(
				request.roomId(),
				request.previousStatus(),
				request.newStatus(),
				request.changeReason(),
				request.changedBy(),
				request.isConflict()
		);
		// save() 를 부르지 않아도 트랜잭션이 끝날 때 바뀐 값이 자동으로 DB 에 반영됩니다. (변경 감지)
		// 다만 수정일시(updatedAt)는 반영되는 순간에 채워지므로, 응답에 새 값을 담으려고 먼저 반영합니다.
		roomStatusLogRepository.flush();
		return RoomStatusLogResponse.from(roomStatusLog);
	}

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private RoomStatusLog findOrThrow(Long id) {
		return roomStatusLogRepository.findById(id)
				.orElseThrow(() -> new BusinessException("객실 상태 변경 이력을 찾을 수 없습니다."));
	}
}

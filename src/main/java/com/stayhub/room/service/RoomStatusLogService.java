package com.stayhub.room.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.room.dto.RoomStatusLogResponse;
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
 * <p>이력을 조회해 응답 DTO 로 바꿔 돌려줍니다. 엔티티는 이 클래스 밖으로 나가지 않습니다.
 * 이력은 {@code RoomService.changeStatus} 가 자동으로 남기므로, 여기에는 등록·수정 메서드가 없습니다.
 *
 * <p><b>트랜잭션 규칙</b> — 조회만 있어서 클래스의 {@code @Transactional(readOnly = true)} 로 충분합니다.
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

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private RoomStatusLog findOrThrow(Long id) {
		return roomStatusLogRepository.findById(id)
				.orElseThrow(() -> new BusinessException("객실 상태 변경 이력을 찾을 수 없습니다."));
	}
}

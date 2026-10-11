package com.stayhub.room.service;

import com.stayhub.common.BusinessException;
import com.stayhub.common.PageResponse;
import com.stayhub.common.PropertyScope;
import com.stayhub.room.dto.RoomCreateRequest;
import com.stayhub.room.dto.RoomResponse;
import com.stayhub.room.dto.RoomUpdateRequest;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
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
 * <p><b>트랜잭션 규칙</b> — 클래스에 {@code @Transactional(readOnly = true)}, 쓰기 메서드(등록·수정)에만 {@code @Transactional}.
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

	private final RoomRepository roomRepository;

	public RoomService(RoomRepository roomRepository) {
		this.roomRepository = roomRepository;
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

	/** 등록. property_id 는 요청에서 받지 않고 여기서 상수로 넣습니다. */
	@Transactional
	public RoomResponse create(RoomCreateRequest request) {
		Room room = Room.create(
				PropertyScope.DEFAULT_PROPERTY_ID,
				request.roomNumber(),
				request.roomType(),
				request.basePrice(),
				request.maxOccupancy()
		);
		return RoomResponse.from(roomRepository.save(room));
	}

	/** 수정. 없는 항목이면 BusinessException. */
	@Transactional
	public RoomResponse update(Long id, RoomUpdateRequest request) {
		Room room = findOrThrow(id);
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

	/** 항목 하나를 찾습니다. 없으면 예외. */
	private Room findOrThrow(Long id) {
		return roomRepository.findById(id)
				.orElseThrow(() -> new BusinessException("객실을 찾을 수 없습니다."));
	}
}

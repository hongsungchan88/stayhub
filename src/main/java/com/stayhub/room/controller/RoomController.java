package com.stayhub.room.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.room.dto.RoomCreateRequest;
import com.stayhub.room.dto.RoomResponse;
import com.stayhub.room.dto.RoomStatusChangeRequest;
import com.stayhub.room.dto.RoomUpdateRequest;
import com.stayhub.room.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 객실 API.
 *
 * <p>HTTP 요청을 받아 서비스에 넘기고, 결과를 {@link ApiResponse} 로 감싸서 돌려줍니다.
 *
 * <pre>
 * GET    /api/rooms?page=0   목록 (page 는 생략 가능)
 * GET    /api/rooms/{id}     상세
 * POST   /api/rooms          등록
 * PUT    /api/rooms/{id}     수정
 * PUT    /api/rooms/{id}/status   상태 변경 (ROM-001. 이력이 room_status_logs 에 남음)
 * </pre>
 * 유형 ②(업무 상태 있음) 테이블이라 DELETE API 는 없습니다.
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

	private final RoomService roomService;

	public RoomController(RoomService roomService) {
		this.roomService = roomService;
	}

	@GetMapping
	public ApiResponse<PageResponse<RoomResponse>> getList(
			@RequestParam(defaultValue = "0") int page
	) {
		return ApiResponse.success(roomService.getList(page));
	}

	@GetMapping("/{id}")
	public ApiResponse<RoomResponse> getDetail(@PathVariable Long id) {
		return ApiResponse.success(roomService.getDetail(id));
	}

	@PostMapping
	public ApiResponse<RoomResponse> create(@Valid @RequestBody RoomCreateRequest request) {
		RoomResponse response = roomService.create(request);
		return ApiResponse.success(response);
	}

	@PutMapping("/{id}")
	public ApiResponse<RoomResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody RoomUpdateRequest request
	) {
		return ApiResponse.success(roomService.update(id, request));
	}

	@PutMapping("/{id}/status")
	public ApiResponse<RoomResponse> changeStatus(
			@PathVariable Long id,
			@Valid @RequestBody RoomStatusChangeRequest request
	) {
		return ApiResponse.success(roomService.changeStatus(id, request));
	}
}

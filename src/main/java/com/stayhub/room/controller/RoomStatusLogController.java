package com.stayhub.room.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.room.dto.RoomStatusLogCreateRequest;
import com.stayhub.room.dto.RoomStatusLogResponse;
import com.stayhub.room.dto.RoomStatusLogUpdateRequest;
import com.stayhub.room.service.RoomStatusLogService;
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
 * 객실 상태 변경 이력 API.
 *
 * <p>HTTP 요청을 받아 서비스에 넘기고, 결과를 {@link ApiResponse} 로 감싸서 돌려줍니다.
 *
 * <pre>
 * GET    /api/room-status-logs?page=0   목록 (page 는 생략 가능)
 * GET    /api/room-status-logs/{id}     상세
 * POST   /api/room-status-logs          등록
 * PUT    /api/room-status-logs/{id}     수정
 * </pre>
 * 삭제 기능이 없는 테이블이라 DELETE API 는 없습니다.
 */
@RestController
@RequestMapping("/api/room-status-logs")
public class RoomStatusLogController {

	private final RoomStatusLogService roomStatusLogService;

	public RoomStatusLogController(RoomStatusLogService roomStatusLogService) {
		this.roomStatusLogService = roomStatusLogService;
	}

	@GetMapping
	public ApiResponse<PageResponse<RoomStatusLogResponse>> getList(
			@RequestParam(defaultValue = "0") int page
	) {
		return ApiResponse.success(roomStatusLogService.getList(page));
	}

	@GetMapping("/{id}")
	public ApiResponse<RoomStatusLogResponse> getDetail(@PathVariable Long id) {
		return ApiResponse.success(roomStatusLogService.getDetail(id));
	}

	@PostMapping
	public ApiResponse<RoomStatusLogResponse> create(@Valid @RequestBody RoomStatusLogCreateRequest request) {
		RoomStatusLogResponse response = roomStatusLogService.create(request);
		return ApiResponse.success(response);
	}

	@PutMapping("/{id}")
	public ApiResponse<RoomStatusLogResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody RoomStatusLogUpdateRequest request
	) {
		return ApiResponse.success(roomStatusLogService.update(id, request));
	}
}

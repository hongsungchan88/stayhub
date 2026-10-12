package com.stayhub.room.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.room.dto.HousekeepingTaskCreateRequest;
import com.stayhub.room.dto.HousekeepingTaskResponse;
import com.stayhub.room.dto.HousekeepingTaskUpdateRequest;
import com.stayhub.room.service.HousekeepingTaskService;
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
 * 청소/점검 작업 스케줄 API.
 *
 * <p>HTTP 요청을 받아 서비스에 넘기고, 결과를 {@link ApiResponse} 로 감싸서 돌려줍니다.
 *
 * <pre>
 * GET    /api/housekeeping-tasks?page=0   목록 (page 는 생략 가능)
 * GET    /api/housekeeping-tasks/{id}     상세
 * POST   /api/housekeeping-tasks          등록
 * PUT    /api/housekeeping-tasks/{id}     수정
 * </pre>
 * 유형 ②(업무 상태 있음) 테이블이라 DELETE API 는 없습니다.
 */
@RestController
@RequestMapping("/api/housekeeping-tasks")
public class HousekeepingTaskController {

	private final HousekeepingTaskService housekeepingTaskService;

	public HousekeepingTaskController(HousekeepingTaskService housekeepingTaskService) {
		this.housekeepingTaskService = housekeepingTaskService;
	}

	@GetMapping
	public ApiResponse<PageResponse<HousekeepingTaskResponse>> getList(
			@RequestParam(defaultValue = "0") int page
	) {
		return ApiResponse.success(housekeepingTaskService.getList(page));
	}

	@GetMapping("/{id}")
	public ApiResponse<HousekeepingTaskResponse> getDetail(@PathVariable Long id) {
		return ApiResponse.success(housekeepingTaskService.getDetail(id));
	}

	@PostMapping
	public ApiResponse<HousekeepingTaskResponse> create(@Valid @RequestBody HousekeepingTaskCreateRequest request) {
		HousekeepingTaskResponse response = housekeepingTaskService.create(request);
		return ApiResponse.success(response);
	}

	@PutMapping("/{id}")
	public ApiResponse<HousekeepingTaskResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody HousekeepingTaskUpdateRequest request
	) {
		return ApiResponse.success(housekeepingTaskService.update(id, request));
	}
}

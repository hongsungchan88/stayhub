package com.stayhub.account.controller;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.dto.UserCreateRequest;
import com.stayhub.account.dto.UserResponse;
import com.stayhub.account.dto.UserUpdateRequest;
import com.stayhub.account.entity.UserRole;
import com.stayhub.account.service.LoginSessionManager;
import com.stayhub.account.service.UserService;
import com.stayhub.common.ApiResponse;
import com.stayhub.common.ForbiddenException;
import com.stayhub.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

/**
 * 직원 계정 관리 API (ACC-005). 호스트만 부를 수 있습니다.
 *
 * <pre>
 * GET    /api/users?page=0   목록 (ACTIVE 만)
 * GET    /api/users/{id}     상세
 * POST   /api/users          직원 계정 등록
 * PUT    /api/users/{id}     수정(이름·역할). 역할이 바뀌면 그 직원 세션을 끊음
 * DELETE /api/users/{id}     삭제(소프트 삭제). 그 직원 세션을 끊음
 * </pre>
 *
 * <p><b>호스트 확인</b> — 모든 메서드 첫 줄에서 {@link #requireHost} 를 부릅니다.
 * 로그인 여부는 인터셉터가 먼저 확인하므로(아니면 401), 여기서는 역할만 봅니다. 호스트가 아니면 403(총괄 확인).
 *
 * <p><b>세션 끊기</b> — 삭제·역할 변경 후 {@code LoginSessionManager.invalidate} 를 부릅니다(총괄 확인).
 * LoginUser 는 다시 로그인할 때까지 바뀌지 않으므로, 끊지 않으면 바뀌기 전 역할로 최대 30분 더 쓸 수 있습니다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;
	private final LoginSessionManager loginSessionManager;

	public UserController(UserService userService, LoginSessionManager loginSessionManager) {
		this.userService = userService;
		this.loginSessionManager = loginSessionManager;
	}

	@GetMapping
	public ApiResponse<PageResponse<UserResponse>> getList(
			@SessionAttribute(LoginUser.SESSION_KEY) LoginUser loginUser,
			@RequestParam(defaultValue = "0") int page
	) {
		requireHost(loginUser);
		return ApiResponse.success(userService.getList(page));
	}

	@GetMapping("/{id}")
	public ApiResponse<UserResponse> getDetail(
			@SessionAttribute(LoginUser.SESSION_KEY) LoginUser loginUser,
			@PathVariable Long id
	) {
		requireHost(loginUser);
		return ApiResponse.success(userService.getDetail(id));
	}

	@PostMapping
	public ApiResponse<UserResponse> create(
			@SessionAttribute(LoginUser.SESSION_KEY) LoginUser loginUser,
			@Valid @RequestBody UserCreateRequest request
	) {
		requireHost(loginUser);
		return ApiResponse.success(userService.create(request));
	}

	@PutMapping("/{id}")
	public ApiResponse<UserResponse> update(
			@SessionAttribute(LoginUser.SESSION_KEY) LoginUser loginUser,
			@PathVariable Long id,
			@Valid @RequestBody UserUpdateRequest request
	) {
		requireHost(loginUser);
		UserRole before = userService.getDetail(id).role();

		UserResponse response = userService.update(loginUser.userId(), id, request);

		if (before != response.role()) {
			loginSessionManager.invalidate(id); // 역할이 바뀌었으면 다시 로그인하게
		}
		return ApiResponse.success(response);
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(
			@SessionAttribute(LoginUser.SESSION_KEY) LoginUser loginUser,
			@PathVariable Long id
	) {
		requireHost(loginUser);
		userService.delete(loginUser.userId(), id);
		loginSessionManager.invalidate(id); // 삭제된 직원이 로그인해 있으면 바로 끊음
		return ApiResponse.success(null);
	}

	/** 호스트가 아니면 403 "권한이 없습니다." */
	private void requireHost(LoginUser loginUser) {
		if (loginUser.role() != UserRole.HOST) {
			throw new ForbiddenException("권한이 없습니다.");
		}
	}
}

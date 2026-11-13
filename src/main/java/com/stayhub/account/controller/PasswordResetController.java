package com.stayhub.account.controller;

import com.stayhub.account.dto.PasswordResetConfirmRequest;
import com.stayhub.account.dto.PasswordResetRequest;
import com.stayhub.account.service.LoginSessionManager;
import com.stayhub.account.service.PasswordResetService;
import com.stayhub.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비밀번호 찾기·재설정 API (ACC-003).
 *
 * <pre>
 * POST /api/password-reset/request   재설정 링크 요청 (토큰은 서버 로그에 출력)
 * POST /api/password-reset/confirm   새 비밀번호 저장 + 기존 세션 만료
 * </pre>
 * 비밀번호를 잊은 사람이 부르므로 로그인 없이 호출됩니다(WebConfig excludePathPatterns, 총괄 확인).
 */
@RestController
@RequestMapping("/api/password-reset")
public class PasswordResetController {

	private final PasswordResetService passwordResetService;
	private final LoginSessionManager loginSessionManager;

	public PasswordResetController(PasswordResetService passwordResetService, LoginSessionManager loginSessionManager) {
		this.passwordResetService = passwordResetService;
		this.loginSessionManager = loginSessionManager;
	}

	@PostMapping("/request")
	public ApiResponse<Void> requestReset(@Valid @RequestBody PasswordResetRequest request) {
		passwordResetService.requestReset(request);
		return ApiResponse.success(null, "가입하신 이메일로 재설정 링크를 보냈습니다. 30분 안에 비밀번호를 바꿔 주세요.");
	}

	@PostMapping("/confirm")
	public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
		Long userId = passwordResetService.resetPassword(request);
		// 다른 곳에 로그인해 있던 세션을 끊습니다(기능 정의서 "재설정 후 기존 세션 만료")
		loginSessionManager.invalidate(userId);
		// 기능 정의서 결과 "비밀번호 재설정 완료 알림"
		return ApiResponse.success(null, "비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해 주세요.");
	}
}

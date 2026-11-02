package com.stayhub.account.controller;

import com.stayhub.account.dto.EmailCodeSendRequest;
import com.stayhub.account.dto.EmailCodeVerifyRequest;
import com.stayhub.account.dto.SignupRequest;
import com.stayhub.account.dto.SignupResponse;
import com.stayhub.account.service.SignupService;
import com.stayhub.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입 API (ACC-001).
 *
 * <pre>
 * GET  /api/signup/check-login-id?loginId=host01   아이디 중복 확인 (중복이면 success:false)
 * POST /api/signup/email-code                      인증코드 발송·재전송 (코드는 서버 로그에 출력)
 * POST /api/signup/email-code/verify               인증코드 확인
 * POST /api/signup                                 회원가입 (인증 완료된 이메일만)
 * </pre>
 */
@RestController
@RequestMapping("/api/signup")
public class SignupController {

	private final SignupService signupService;

	public SignupController(SignupService signupService) {
		this.signupService = signupService;
	}

	@GetMapping("/check-login-id")
	public ApiResponse<Void> checkLoginId(@RequestParam String loginId) {
		signupService.checkLoginId(loginId);
		return ApiResponse.success(null);
	}

	@PostMapping("/email-code")
	public ApiResponse<Void> sendEmailCode(@Valid @RequestBody EmailCodeSendRequest request) {
		signupService.sendEmailCode(request);
		return ApiResponse.success(null);
	}

	@PostMapping("/email-code/verify")
	public ApiResponse<Void> verifyEmailCode(@Valid @RequestBody EmailCodeVerifyRequest request) {
		signupService.verifyEmailCode(request);
		return ApiResponse.success(null);
	}

	@PostMapping
	public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ApiResponse.success(signupService.signup(request));
	}
}

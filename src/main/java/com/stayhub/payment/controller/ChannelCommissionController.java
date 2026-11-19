package com.stayhub.payment.controller;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import com.stayhub.channel.dto.ChannelResponse;
import com.stayhub.common.ApiResponse;
import com.stayhub.common.ForbiddenException;
import com.stayhub.payment.dto.ChannelCommissionUpdateRequest;
import com.stayhub.payment.service.ChannelCommissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

/**
 * 채널 수수료율 수정 API (PAY-003, SCR-PAY-03).
 *
 * <pre>
 * PUT /api/channel-commissions/{id}   채널 id 의 수수료율·기본 수수료율 수정 (호스트만)
 * </pre>
 * 목록은 채널 도메인의 {@code GET /api/channels} 를 그대로 씁니다. 채널 등록·삭제는 이 API 범위가 아닙니다.
 * 화면목록정의서의 접근 권한이 "호스트"라 호스트가 아니면 403 입니다.
 */
@RestController
@RequestMapping("/api/channel-commissions")
public class ChannelCommissionController {

	private final ChannelCommissionService channelCommissionService;

	public ChannelCommissionController(ChannelCommissionService channelCommissionService) {
		this.channelCommissionService = channelCommissionService;
	}

	@PutMapping("/{id}")
	public ApiResponse<ChannelResponse> update(
			@SessionAttribute(LoginUser.SESSION_KEY) LoginUser loginUser,
			@PathVariable Long id,
			@Valid @RequestBody ChannelCommissionUpdateRequest request
	) {
		requireHost(loginUser);
		return ApiResponse.success(channelCommissionService.update(id, request));
	}

	/** 호스트가 아니면 403 "권한이 없습니다." */
	private void requireHost(LoginUser loginUser) {
		if (loginUser.role() != UserRole.HOST) {
			throw new ForbiddenException("권한이 없습니다.");
		}
	}
}

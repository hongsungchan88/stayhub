package com.stayhub.payment.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.payment.dto.PaymentCreateRequest;
import com.stayhub.payment.dto.PaymentResponse;
import com.stayhub.payment.dto.PaymentUpdateRequest;
import com.stayhub.payment.service.PaymentService;
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
 * 결제/환불 API.
 *
 * <p>HTTP 요청을 받아 서비스에 넘기고, 결과를 {@link ApiResponse} 로 감싸서 돌려줍니다.
 *
 * <pre>
 * GET    /api/payments?page=0   목록 (page 는 생략 가능)
 * GET    /api/payments/{id}     상세
 * POST   /api/payments          등록
 * PUT    /api/payments/{id}     수정
 * </pre>
 * 삭제 기능이 없는 테이블이라 DELETE API 는 없습니다.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@GetMapping
	public ApiResponse<PageResponse<PaymentResponse>> getList(
			@RequestParam(defaultValue = "0") int page
	) {
		return ApiResponse.success(paymentService.getList(page));
	}

	@GetMapping("/{id}")
	public ApiResponse<PaymentResponse> getDetail(@PathVariable Long id) {
		return ApiResponse.success(paymentService.getDetail(id));
	}

	@PostMapping
	public ApiResponse<PaymentResponse> create(@Valid @RequestBody PaymentCreateRequest request) {
		PaymentResponse response = paymentService.create(request);
		return ApiResponse.success(response);
	}

	@PutMapping("/{id}")
	public ApiResponse<PaymentResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody PaymentUpdateRequest request
	) {
		return ApiResponse.success(paymentService.update(id, request));
	}
}

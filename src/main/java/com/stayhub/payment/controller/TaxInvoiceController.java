package com.stayhub.payment.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.payment.dto.TaxInvoiceCreateRequest;
import com.stayhub.payment.dto.TaxInvoiceResponse;
import com.stayhub.payment.dto.TaxInvoiceUpdateRequest;
import com.stayhub.payment.service.TaxInvoiceService;
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
 * 세금 계산서/현금 영수증 API.
 *
 * <p>HTTP 요청을 받아 서비스에 넘기고, 결과를 {@link ApiResponse} 로 감싸서 돌려줍니다.
 *
 * <pre>
 * GET    /api/tax-invoices?page=0   목록 (page 는 생략 가능)
 * GET    /api/tax-invoices/{id}     상세
 * POST   /api/tax-invoices          등록
 * PUT    /api/tax-invoices/{id}     수정
 * </pre>
 * 삭제 기능이 없는 테이블이라 DELETE API 는 없습니다.
 */
@RestController
@RequestMapping("/api/tax-invoices")
public class TaxInvoiceController {

	private final TaxInvoiceService taxInvoiceService;

	public TaxInvoiceController(TaxInvoiceService taxInvoiceService) {
		this.taxInvoiceService = taxInvoiceService;
	}

	@GetMapping
	public ApiResponse<PageResponse<TaxInvoiceResponse>> getList(
			@RequestParam(defaultValue = "0") int page
	) {
		return ApiResponse.success(taxInvoiceService.getList(page));
	}

	@GetMapping("/{id}")
	public ApiResponse<TaxInvoiceResponse> getDetail(@PathVariable Long id) {
		return ApiResponse.success(taxInvoiceService.getDetail(id));
	}

	@PostMapping
	public ApiResponse<TaxInvoiceResponse> create(@Valid @RequestBody TaxInvoiceCreateRequest request) {
		TaxInvoiceResponse response = taxInvoiceService.create(request);
		return ApiResponse.success(response);
	}

	@PutMapping("/{id}")
	public ApiResponse<TaxInvoiceResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody TaxInvoiceUpdateRequest request
	) {
		return ApiResponse.success(taxInvoiceService.update(id, request));
	}
}

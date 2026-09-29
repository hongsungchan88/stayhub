package com.stayhub.content.controller;

import com.stayhub.common.ApiResponse;
import com.stayhub.common.PageResponse;
import com.stayhub.content.dto.GuidebookCreateRequest;
import com.stayhub.content.dto.GuidebookResponse;
import com.stayhub.content.dto.GuidebookUpdateRequest;
import com.stayhub.content.entity.GuidebookCategory;
import com.stayhub.content.service.GuidebookService;
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

/**
 * 가이드북 API.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>HTTP 요청을 받아 서비스에 넘기고, 결과를 {@link ApiResponse} 로 감싸서 돌려줍니다. 그게 전부입니다.
 * 업무 로직은 서비스에 두고, 엔티티는 여기서 절대 다루지 않습니다. (응답 DTO 만 오갑니다)
 *
 * <pre>
 * GET    /api/guidebooks?category=WIFI&amp;page=0   목록 (category·page 는 생략 가능)
 * GET    /api/guidebooks/{id}                    상세
 * POST   /api/guidebooks                         등록
 * PUT    /api/guidebooks/{id}                    수정
 * DELETE /api/guidebooks/{id}                    삭제 (소프트 삭제)
 * </pre>
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <ul>
 *   <li>{@code @RequestMapping} 주소 — {@code /api/} + 테이블명. 예) {@code /api/rooms}</li>
 *   <li>클래스명·서비스 타입·DTO 타입 — Guidebook 을 내 테이블 이름으로</li>
 *   <li>목록의 {@code category} 파라미터 — 내 테이블에 필터가 없으면 지웁니다</li>
 *   <li>등록의 중복 제목 경고 — 가이드북만의 규칙입니다. 내 테이블에 없으면 표시된 줄을 지우고
 *       {@code return ApiResponse.success(response);} 한 줄만 남깁니다</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/guidebooks") // ★ 바꿀 곳: API 주소
public class GuidebookController { // ★ 바꿀 곳: 클래스명

	private final GuidebookService guidebookService; // ★ 바꿀 곳: 서비스 타입·이름

	public GuidebookController(GuidebookService guidebookService) { // ★ 바꿀 곳: 클래스명·서비스
		this.guidebookService = guidebookService;
	}

	@GetMapping
	public ApiResponse<PageResponse<GuidebookResponse>> getList( // ★ 바꿀 곳: 응답 타입
			@RequestParam(required = false) GuidebookCategory category, // ★ 바꿀 곳: 필터 (없으면 이 줄 삭제)
			@RequestParam(defaultValue = "0") int page
	) {
		return ApiResponse.success(guidebookService.getList(category, page));
	}

	@GetMapping("/{id}")
	public ApiResponse<GuidebookResponse> getDetail(@PathVariable Long id) { // ★ 바꿀 곳: 응답 타입
		return ApiResponse.success(guidebookService.getDetail(id));
	}

	@PostMapping
	public ApiResponse<GuidebookResponse> create(@Valid @RequestBody GuidebookCreateRequest request) { // ★ 바꿀 곳: 요청·응답 타입
		// 저장하기 전에 확인해야 합니다. 저장한 뒤에 확인하면 방금 저장한 자기 자신이 걸립니다.
		boolean sameTitleExists = guidebookService.hasSameTitle(request.category(), request.title()); // ★ 바꿀 곳: 중복 규칙 없으면 삭제

		GuidebookResponse response = guidebookService.create(request); // ★ 바꿀 곳: 응답 타입

		if (sameTitleExists) { // ★ 바꿀 곳: 중복 규칙 없으면 이 if 문째 삭제
			return ApiResponse.success(response, "같은 카테고리에 같은 제목이 이미 있습니다.");
		}
		return ApiResponse.success(response);
	}

	@PutMapping("/{id}")
	public ApiResponse<GuidebookResponse> update( // ★ 바꿀 곳: 응답 타입
			@PathVariable Long id,
			@Valid @RequestBody GuidebookUpdateRequest request // ★ 바꿀 곳: 요청 타입
	) {
		return ApiResponse.success(guidebookService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		guidebookService.delete(id);
		return ApiResponse.success(null);
	}
}

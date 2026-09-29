package com.stayhub.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 목록 API 의 공통 응답 형식.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>목록 조회 결과를 아래 다섯 값으로 내려보냅니다. {@code ApiResponse} 의 data 자리에 들어갑니다.
 * <pre>
 * {
 *   "content": [ ... ],     이번 페이지의 항목들
 *   "page": 0,              현재 페이지 번호 (0부터 시작)
 *   "size": 20,             한 페이지 크기
 *   "totalElements": 45,    전체 항목 수
 *   "totalPages": 3         전체 페이지 수
 * }
 * </pre>
 *
 * <p>Spring 의 {@code Page} 를 그대로 반환하지 않는 이유: 내부 필드가 잔뜩 딸려 나가고,
 * Spring 버전이 바뀌면 응답 모양이 달라질 수 있습니다. 필요한 값만 골라 고정해 둡니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>없습니다. 복사하지 말고 그대로 가져다 쓰세요.
 * 서비스에서 {@code PageResponse.from(page.map(XxxResponse::from))} 처럼 씁니다.
 *
 * @param <T> 목록에 담길 응답 DTO 타입
 */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages
) {

	/** 정책: 목록은 한 페이지에 20건. 모든 목록 API 가 이 값을 씁니다. */
	public static final int DEFAULT_PAGE_SIZE = 20;

	/** Spring Data 의 Page 를 공통 응답 형식으로 바꿉니다. */
	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages()
		);
	}
}

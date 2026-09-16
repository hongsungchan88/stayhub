package com.stayhub.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모든 API 응답의 공통 형식.
 *
 * <p>응답 모양을 팀 전체가 통일하기 위해 씁니다. 프런트에서 매번 다른 구조를
 * 파싱하지 않아도 되도록, 성공이든 실패든 아래 세 필드로 내려보냅니다.
 *
 * <pre>
 * { "success": true,  "data": { ... }, "message": null }
 * { "success": false, "data": null,    "message": "예약을 찾을 수 없습니다." }
 * </pre>
 *
 * <p>사용 예시
 * <pre>
 * return ApiResponse.success(reservationDto);
 * return ApiResponse.success(reservationDto, "예약이 등록되었습니다.");
 * return ApiResponse.error("예약을 찾을 수 없습니다.");
 * </pre>
 *
 * @param <T> 응답으로 내려보낼 데이터 타입
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

	/** 성공 여부. 실패면 false. */
	private final boolean success;

	/** 실제 데이터. 실패했거나 돌려줄 데이터가 없으면 null. */
	private final T data;

	/** 안내 메시지. 없으면 null. 실패한 경우 여기에 사유가 담깁니다. */
	private final String message;

	/** 성공 응답을 만듭니다. */
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null);
	}

	/** 안내 메시지를 함께 담은 성공 응답을 만듭니다. */
	public static <T> ApiResponse<T> success(T data, String message) {
		return new ApiResponse<>(true, data, message);
	}

	/** 실패 응답을 만듭니다. data 는 null 로 내려갑니다. */
	public static <T> ApiResponse<T> error(String message) {
		return new ApiResponse<>(false, null, message);
	}
}

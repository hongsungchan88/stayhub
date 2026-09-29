package com.stayhub.common;

/**
 * 현재 작업 대상 숙소.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>정책: 1차 개발 범위는 단일 숙소 고정입니다. 그래서 {@code property_id} 는 항상 1 입니다.
 *
 * <p>{@code property_id} 는 요청(화면)에서 받지 않습니다. 서비스에서 이 상수를 넣습니다.
 * 요청에서 받으면 누군가 값을 바꿔 보내서 다른 숙소의 데이터를 건드릴 수 있습니다.
 * 나중에 여러 숙소를 지원하게 되면 이 파일 한 곳만 바꾸면 되도록 모아둔 것입니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>없습니다. 복사하지 말고 그대로 가져다 쓰세요.
 */
public final class PropertyScope {

	/** 1차 개발 범위의 숙소 ID. */
	public static final Long DEFAULT_PROPERTY_ID = 1L;

	private PropertyScope() {
		// 상수만 담는 클래스라 객체를 만들 일이 없습니다.
	}
}

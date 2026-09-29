package com.stayhub.common;

/**
 * 현재 작업 대상 숙소.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>정책: 1차 개발 범위는 단일 숙소 고정입니다. 그래서 {@code property_id} 는 항상 1 입니다.
 *
 * <p>{@code property_id} 가 있는 4개 테이블(rooms, property_images, property_facilities, guidebooks)은
 * 등록할 때 서비스에서 이 상수를 넣습니다. 요청(화면)에서는 받지 않습니다.
 * properties 는 property_id 가 자기 PK(AUTO_INCREMENT)라 해당 없습니다. 이 상수를 넣지 않습니다.
 * 요청에서 받으면 누군가 값을 바꿔 보내서 다른 숙소의 데이터를 건드릴 수 있습니다.
 * 조회 조건에는 넣지 않습니다. 1차는 숙소가 하나뿐이라 조건을 걸 필요가 없습니다(정책정의서 32행).
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

package com.stayhub.common;

/**
 * 소프트 삭제 전용 상태값.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>행을 DB에서 실제로 지우지 않고 {@code DELETED} 로 표시만 해두기 위한 값입니다.
 * 테이블 정의서에 {@code status VARCHAR(20) — ACTIVE / DELETED} 가 있는 테이블은
 * 모두 이 enum 을 씁니다.
 *
 * <p><b>이 enum 은 소프트 삭제 전용입니다.</b>
 * 예약 상태(확정·취소·체크인 등) 같은 업무 상태는 여기에 값을 추가하지 말고,
 * 도메인별 enum 으로 따로 만드세요. 예) {@code reservation/entity/ReservationStatus}
 * 여기에 업무 상태를 섞으면 모든 테이블이 서로의 상태값을 갖게 됩니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>없습니다. 복사하지 말고 그대로 가져다 쓰세요.
 */
public enum EntityStatus {

	/** 사용 중. 목록·상세 조회에 나옵니다. */
	ACTIVE,

	/** 삭제됨. 행은 남아 있지만 목록·상세 조회에서 보이지 않습니다. */
	DELETED
}

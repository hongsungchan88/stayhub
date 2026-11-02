package com.stayhub.reservation.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * 예약 상태와 상태 전이 규칙 (RSV-001~004).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>테이블 정의서의 {@code status VARCHAR(20) — CONFIRMED, CANCELLED, NOSHOW} 에
 * 체크인·체크아웃(RSV-003·004)용 {@code CHECKED_IN}, {@code CHECKED_OUT} 을 더한 것입니다(총괄 결정).
 * DB 에는 이름 그대로 저장됩니다.
 *
 * <p><b>바꿀 수 있는 상태는 이 파일의 {@link #canChangeTo} 한 곳에서만 정합니다.</b>
 * <pre>
 * CONFIRMED  → CANCELLED · NOSHOW · CHECKED_IN
 * CHECKED_IN → CHECKED_OUT
 * CANCELLED · NOSHOW · CHECKED_OUT → (끝. 더 바꿀 수 없음)
 * </pre>
 * 서비스에서 if 문으로 따로 막지 말고 {@code Reservation.changeStatus} 를 부르세요. 규칙이 두 곳에 있으면 언젠가 어긋납니다.
 */
public enum ReservationStatus {

	/** 예약 완료. 새 예약은 항상 이 상태로 시작합니다 */
	CONFIRMED,

	/** 취소 */
	CANCELLED,

	/** 노쇼 */
	NOSHOW,

	/** 체크인 완료 (투숙 중) */
	CHECKED_IN,

	/** 체크아웃 완료 */
	CHECKED_OUT;

	/** 객실을 다시 내준 상태. 이 상태의 예약과는 날짜가 겹쳐도 새 예약을 받습니다. */
	public static final Set<ReservationStatus> RELEASED = EnumSet.of(CANCELLED, NOSHOW);

	/** 지금 상태에서 {@code next} 로 바꿀 수 있는지. */
	public boolean canChangeTo(ReservationStatus next) {
		return switch (this) {
			case CONFIRMED -> next == CANCELLED || next == NOSHOW || next == CHECKED_IN;
			case CHECKED_IN -> next == CHECKED_OUT;
			case CANCELLED, NOSHOW, CHECKED_OUT -> false;
		};
	}
}

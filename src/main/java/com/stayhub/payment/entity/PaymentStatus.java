package com.stayhub.payment.entity;

/**
 * 결제 상태.
 *
 * <p>테이블 정의서의 {@code payment_status VARCHAR(20) — COMPLETED, FAILED, REFUND_REQUESTED, REFUNDED, PARTIALLY_REFUNDED} 를 옮긴 것입니다.
 * 업무 상태라서 {@code common/EntityStatus} 를 쓰지 않고 결제 도메인 enum 으로 따로 둡니다.
 *
 * <p><b>바꿀 수 있는 상태는 {@link #canChangeTo} 한 곳에서만 정합니다</b> (정책정의서 39행 — 총괄 틀).
 * <pre>
 * COMPLETED        → REFUND_REQUESTED
 * REFUND_REQUESTED → REFUNDED · PARTIALLY_REFUNDED · COMPLETED(환불 거절)
 * FAILED · REFUNDED · PARTIALLY_REFUNDED → (끝. 더 바꿀 수 없음)
 * </pre>
 * 상태를 바꿀 때는 {@code Payment.changeStatus} 를 부르세요. 서비스에서 if 문으로 따로 막지 않습니다.
 */
public enum PaymentStatus {

	/** 결제 완료 */
	COMPLETED,

	/** 결제 실패 */
	FAILED,

	/** 환불 요청 */
	REFUND_REQUESTED,

	/** 환불 완료 */
	REFUNDED,

	/** 부분 환불 */
	PARTIALLY_REFUNDED;

	/** 지금 상태에서 {@code next} 로 바꿀 수 있는지. */
	public boolean canChangeTo(PaymentStatus next) {
		return switch (this) {
			case COMPLETED -> next == REFUND_REQUESTED;
			case REFUND_REQUESTED -> next == REFUNDED || next == PARTIALLY_REFUNDED || next == COMPLETED;
			case FAILED, REFUNDED, PARTIALLY_REFUNDED -> false;
		};
	}
}

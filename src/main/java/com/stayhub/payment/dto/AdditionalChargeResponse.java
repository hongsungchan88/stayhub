package com.stayhub.payment.dto;

import com.stayhub.payment.entity.AdditionalCharge;
import com.stayhub.payment.entity.AdditionalChargeType;
import com.stayhub.reservation.entity.Reservation;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 추가 요금 응답 (목록·상세·등록·수정이 모두 이 모양으로 나갑니다).
 *
 * <p>reservationId·paymentId 는 화면에 필요한 참조 ID 라서 넣습니다.
 * reservationCode·guestName은 표의 예약 칸에 보여 줄 값입니다. 화면이 줄마다 예약을 따로 부르지 않도록
 * 서비스가 한 페이지의 예약을 한 번에 찾아 채웁니다. 예약을 찾지 못하면 null입니다.
 * 변환({@link #from})은 서비스 안에서 호출합니다. 컨트롤러는 엔티티를 만지지 않습니다.
 */
public record AdditionalChargeResponse(
		Long id,
		Long reservationId,
		String reservationCode,
		String guestName,
		AdditionalChargeType chargeType,
		String description,
		BigDecimal amount,
		boolean isMerged,
		Long paymentId,
		boolean isSettled,
		LocalDateTime chargedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	/** 엔티티를 응답 모양으로 바꿉니다. reservation이 null이면 예약 번호·손님 이름도 null입니다. */
	public static AdditionalChargeResponse from(AdditionalCharge additionalCharge, Reservation reservation) {
		return new AdditionalChargeResponse(
				additionalCharge.getId(),
				additionalCharge.getReservationId(),
				reservation == null ? null : reservation.getReservationCode(),
				reservation == null ? null : reservation.getGuestName(),
				additionalCharge.getChargeType(),
				additionalCharge.getDescription(),
				additionalCharge.getAmount(),
				additionalCharge.isMerged(),
				additionalCharge.getPaymentId(),
				additionalCharge.isSettled(),
				additionalCharge.getChargedAt(),
				additionalCharge.getCreatedAt(),
				additionalCharge.getUpdatedAt()
		);
	}
}

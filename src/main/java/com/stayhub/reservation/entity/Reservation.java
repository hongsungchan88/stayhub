package com.stayhub.reservation.entity;

import com.stayhub.common.BaseEntity;
import com.stayhub.common.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 예약 (테이블 {@code reservations}). 테이블 유형 ②(업무 상태 있음).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>테이블 정의서 reservations 의 컬럼을 필드로 옮긴 것입니다. {@code created_at}·{@code updated_at} 은 {@link BaseEntity} 가 채웁니다.
 * <ul>
 *   <li>만들기 — {@link #create}. <b>직접 부르지 말고 {@code ReservationService.create} 를 거치세요.</b>
 *       날짜 겹침 검사와 동시 예약 차단이 서비스에만 있습니다</li>
 *   <li>상태 바꾸기 — {@link #changeStatus}. 허용되는 변경은 {@link ReservationStatus#canChangeTo} 에 있습니다</li>
 * </ul>
 * 삭제 기능은 없습니다(유형 ②). 취소는 {@code CANCELLED} 로 상태를 바꾸는 것입니다.
 *
 * <p><b>담당자(B)가 채울 곳</b>
 * <br>{@code cancel_reason} 은 컬럼만 있고 값을 넣는 메서드가 없습니다. 취소(RSV-002)를 만들 때
 * {@code cancel(String reason)} 처럼 상태 변경과 사유를 함께 넣는 메서드를 여기에 추가하세요.
 * 예약 변경(날짜·객실 바꾸기)도 겹침 검사가 필요하므로 서비스에서 {@code create} 와 같은 방식으로 막아야 합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "reservations")
public class Reservation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "reservation_id")
	private Long id;

	@Column(name = "reservation_code", nullable = false, length = 50, unique = true) // 정의서의 UNIQUE
	private String reservationCode;

	/** 회원 예약이면 users.user_id, 비회원 예약이면 NULL. */
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "guest_name", nullable = false, length = 50)
	private String guestName;

	@Column(name = "guest_phone", nullable = false, length = 20)
	private String guestPhone;

	@Column(name = "room_id", nullable = false)
	private Long roomId;

	/** 예약 채널(channels). 직접 예약이면 NULL. */
	@Column(name = "channel_id")
	private Long channelId;

	@Column(name = "check_in_date", nullable = false)
	private LocalDate checkInDate;

	/** 이 날 아침 퇴실. 같은 객실의 다음 예약은 이 날 체크인할 수 있습니다. */
	@Column(name = "check_out_date", nullable = false)
	private LocalDate checkOutDate;

	@ColumnDefault("1") // 정의서의 "기본 1"
	@Column(name = "guest_count", nullable = false)
	private int guestCount;

	@Enumerated(EnumType.STRING)
	@ColumnDefault("'CONFIRMED'") // 정의서의 기본값
	@Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20)")
	private ReservationStatus status;

	@Column(name = "cancel_reason", length = 255)
	private String cancelReason;

	/**
	 * 동시 변경을 막는 버전 번호(정책 40행). JPA 가 저장할 때마다 올립니다. 직접 바꾸지 않습니다.
	 * 두 사람이 같은 예약의 상태를 거의 동시에 바꾸면 나중 쪽은 ObjectOptimisticLockingFailureException(409)으로 실패합니다.
	 */
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	/** 새 예약. 상태는 항상 CONFIRMED 로 시작합니다. 검증은 서비스가 먼저 합니다. */
	public static Reservation create(
			String reservationCode,
			Long userId,
			String guestName,
			String guestPhone,
			Long roomId,
			Long channelId,
			LocalDate checkInDate,
			LocalDate checkOutDate,
			Integer guestCount
	) {
		Reservation reservation = new Reservation();
		reservation.reservationCode = reservationCode;
		reservation.userId = userId;
		reservation.guestName = guestName;
		reservation.guestPhone = guestPhone;
		reservation.roomId = roomId;
		reservation.channelId = channelId;
		reservation.checkInDate = checkInDate;
		reservation.checkOutDate = checkOutDate;
		reservation.guestCount = guestCount == null ? 1 : guestCount;
		reservation.status = ReservationStatus.CONFIRMED;
		return reservation;
	}

	/** 상태 변경. 허용되지 않는 변경이면 BusinessException. */
	public void changeStatus(ReservationStatus next) {
		if (!status.canChangeTo(next)) {
			throw new BusinessException("예약 상태를 " + status + "에서 " + next + "(으)로 바꿀 수 없습니다.");
		}
		this.status = next;
	}
}

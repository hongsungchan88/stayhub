package com.stayhub.account.entity;

import com.stayhub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 이메일 인증/재설정 기록 (테이블 {@code email_verifications}). 유형 ③ — status 가 없고 삭제 기능도 없습니다.
 *
 * <p>회원가입 인증코드(24시간 유효)와 비밀번호 재설정 토큰(30분 유효, ACC-003)을 함께 관리합니다.
 * 아직 회원이 아닌 상태에서 인증하기 때문에 {@code user_id} 가 아니라 {@code email} 로 기록합니다.
 *
 * <p>{@code created_at} 은 {@link BaseEntity} 가 채웁니다. updated_at 은 정의서에 없지만 BaseEntity 상속으로 생깁니다(총괄 허용).
 *
 * <p>값을 바꾸는 방법은 {@link #create} / {@link #markUsed} / {@link #recordFailedAttempt} 뿐입니다.
 *
 * <p><b>시각은 파라미터로 받습니다.</b> 총괄의 {@code User.isLocked(now)} 와 같은 방식입니다.
 * 테스트에서 "24시간 뒤"를 실제로 기다리지 않고 확인하기 위해서입니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "email_verifications")
public class EmailVerification extends BaseEntity {

	/** 회원가입 인증코드 유효 시간. (정의서 테이블 설명 — 24시간 유효) */
	public static final long JOIN_VALID_HOURS = 24;

	/** 비밀번호 재설정 토큰 유효 시간. (정의서 테이블 설명·ACC-003 — 30분 유효) */
	public static final long PASSWORD_RESET_VALID_MINUTES = 30;

	/** 회원가입 인증코드를 이 횟수만큼 틀리면 그 코드는 무효입니다. (ACC-003 작업 때 총괄 확인) */
	public static final int MAX_FAILED_ATTEMPTS = 5;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "verification_id")
	private Long id;

	@Column(name = "email", nullable = false, length = 100)
	private String email;

	/** 인증코드(가입) 또는 재설정 토큰. */
	@Column(name = "code", nullable = false, length = 255)
	private String code;

	@Enumerated(EnumType.STRING)
	@ColumnDefault("'JOIN'") // 정의서의 기본값 "JOIN"
	@Column(name = "type", nullable = false, columnDefinition = "VARCHAR(20)")
	private VerificationType type;

	/** 이 시각이 지나면 쓸 수 없습니다. 발송 시 현재시간 + 유효시간. */
	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	/**
	 * 사용 완료 여부. 필드명은 {@code used}, getter 는 Lombok 규칙대로 {@code isUsed()} 입니다. (User 의 verified 와 같은 방식)
	 * columnDefinition = "BOOLEAN" — 빼면 MySQL 에 bit(1) 로 만들어져 정의서(BOOLEAN = tinyint(1))와 달라집니다.
	 */
	@ColumnDefault("false") // 정의서의 "기본 FALSE"
	@Column(name = "is_used", nullable = false, columnDefinition = "BOOLEAN")
	private boolean used;

	/**
	 * 인증코드를 틀린 횟수. {@link #MAX_FAILED_ATTEMPTS} 가 되면 그 코드는 맞는 코드로도 쓸 수 없습니다.
	 * 정의서에 없던 컬럼입니다 — users.failed_attempts 와 같은 이름·방식으로 추가했습니다(PR 에 기재, 정의서 반영 필요).
	 * 재전송하면 새 행이 생기므로 횟수도 0 부터 다시 셉니다.
	 */
	@ColumnDefault("0")
	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;

	/** 새 인증 기록을 만듭니다. 사용 전(false)·틀린 횟수 0 으로 시작합니다. */
	public static EmailVerification create(
			String email,
			String code,
			VerificationType type,
			LocalDateTime expiresAt
	) {
		EmailVerification verification = new EmailVerification();
		verification.email = email;
		verification.code = code;
		verification.type = type;
		verification.expiresAt = expiresAt;
		verification.used = false;
		verification.failedAttempts = 0;
		return verification;
	}

	/** 인증 완료 표시. */
	public void markUsed() {
		this.used = true;
	}

	/** 인증코드를 한 번 틀렸다고 기록합니다. */
	public void recordFailedAttempt() {
		this.failedAttempts++;
	}

	/** 틀린 횟수가 한도에 닿아 더 이상 쓸 수 없는지. */
	public boolean isAttemptsExceeded() {
		return failedAttempts >= MAX_FAILED_ATTEMPTS;
	}

	/** {@code now} 시점에 만료됐는지. */
	public boolean isExpired(LocalDateTime now) {
		return !now.isBefore(expiresAt);
	}
}

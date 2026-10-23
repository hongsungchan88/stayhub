package com.stayhub.account.entity;

import com.stayhub.common.BaseEntity;
import com.stayhub.common.EntityStatus;
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
 * 사용자/직원 계정 (테이블 {@code users}).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>테이블 정의서 users 의 13개 컬럼을 필드로 옮긴 것입니다.
 * {@code created_at} / {@code updated_at} 은 {@link BaseEntity} 가 채웁니다.
 * 테이블 유형 ①(소프트 삭제 — {@link EntityStatus} ACTIVE/DELETED, 정책정의서 24행)입니다.
 *
 * <p>값을 바꾸는 방법은 아래 메서드뿐입니다.
 * <ul>
 *   <li>가입·직원 계정 생성 — {@link #create} (비밀번호는 <b>반드시 암호화한 값</b>을 넘깁니다)</li>
 *   <li>이메일 인증 완료 — {@link #verifyEmail}</li>
 *   <li>로그인 실패 — {@link #recordLoginFailure} (5회째에 30분 잠금)</li>
 *   <li>로그인 성공 — {@link #resetLoginFailures}</li>
 *   <li>잠겨 있는지 확인 — {@link #isLocked}</li>
 *   <li>삭제 — {@link #delete} (행은 남기고 status 만 DELETED)</li>
 * </ul>
 *
 * <p><b>따라 만들 때 바꿀 곳 (회원가입 담당자용)</b>
 * <br>이 파일은 고치지 않고 그대로 씁니다. 회원가입 서비스에서는 이렇게 부릅니다.
 * <pre>
 * if (userRepository.existsByLoginId(loginId)) → 중복 아이디 예외
 * if (userRepository.existsByEmail(email))     → 중복 이메일 예외
 * User user = User.create(loginId, passwordEncoder.encode(rawPassword), name, phoneNumber, email, null);
 * userRepository.save(user);
 * </pre>
 * 형식 검증(아이디 4~20자 영문+숫자, 비밀번호 8자 이상 + 특수문자)은 요청 DTO 의 {@code @Pattern}·{@code @Size} 로 합니다.
 * 이 엔티티에는 암호화된 비밀번호만 들어오기 때문에 여기서는 검사할 수 없습니다.
 *
 * <p><b>시각은 파라미터로 받습니다.</b>
 * {@code recordLoginFailure}·{@code isLocked} 가 {@code LocalDateTime.now()} 를 직접 부르지 않는 이유:
 * 테스트에서 "30분 뒤"를 실제로 기다리지 않고 시각을 넘겨서 확인하기 위해서입니다.
 * 서비스에서는 {@code LocalDateTime.now()} 를 넘기면 됩니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "users")
public class User extends BaseEntity {

	/** 이 횟수만큼 연속으로 틀리면 잠급니다. (정의서 failed_attempts 비고) */
	public static final int MAX_FAILED_ATTEMPTS = 5;

	/** 잠금 시간(분). (정의서 lockout_until 비고 — 현재시간 + 30분) */
	public static final long LOCKOUT_MINUTES = 30;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "user_id")
	private Long id;

	@Column(name = "login_id", nullable = false, length = 20, unique = true) // 정의서: UNIQUE
	private String loginId;

	/** BCrypt 로 암호화된 값. 원문 비밀번호는 저장하지 않습니다. */
	@Column(name = "password", nullable = false, length = 255)
	private String password;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "phone_number", nullable = false, length = 20)
	private String phoneNumber;

	@Column(name = "email", nullable = false, length = 100, unique = true) // 정의서: UNIQUE
	private String email;

	@Enumerated(EnumType.STRING)
	@ColumnDefault("'STAFF'")
	@Column(name = "role", nullable = false, columnDefinition = "VARCHAR(20)")
	private UserRole role;

	/** 이메일 인증 완료 여부. 필드명은 {@code verified}, getter 는 Lombok 규칙대로 {@code isVerified()} 입니다. */
	@ColumnDefault("false")
	@Column(name = "is_verified", nullable = false)
	private boolean verified;

	@ColumnDefault("0")
	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;

	/** 이 시각 전까지 로그인 불가. 잠겨 있지 않으면 NULL. */
	@Column(name = "lockout_until")
	private LocalDateTime lockoutUntil;

	@Enumerated(EnumType.STRING)
	@ColumnDefault("'ACTIVE'")
	@Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20)")
	private EntityStatus status;

	/**
	 * 새 계정을 만듭니다. 미인증·실패 0회·ACTIVE 로 시작합니다.
	 *
	 * @param encodedPassword {@code PasswordEncoder.encode()} 를 거친 값. 원문을 넣지 마세요.
	 * @param role            비어 있으면 STAFF (정의서 기본값)
	 */
	public static User create(
			String loginId,
			String encodedPassword,
			String name,
			String phoneNumber,
			String email,
			UserRole role
	) {
		User user = new User();
		user.loginId = loginId;
		user.password = encodedPassword;
		user.name = name;
		user.phoneNumber = phoneNumber;
		user.email = email;
		user.role = role == null ? UserRole.STAFF : role;
		user.verified = false;
		user.failedAttempts = 0;
		user.status = EntityStatus.ACTIVE;
		return user;
	}

	/** 이메일 인증 완료 표시. */
	public void verifyEmail() {
		this.verified = true;
	}

	/**
	 * 로그인 실패를 한 번 기록합니다. 5회째이면 {@code now + 30분} 까지 잠그고 횟수를 0 으로 돌립니다.
	 * 횟수를 돌리는 이유: 잠금이 풀린 뒤 다시 "5회 연속" 실패해야 잠기도록 하기 위해서입니다.
	 * (돌리지 않으면 잠금이 풀린 뒤 한 번만 틀려도 바로 다시 잠깁니다)
	 * 잠겨 있는 동안에는 서비스가 비밀번호 비교 전에 {@link #isLocked} 로 먼저 막아야 합니다.
	 */
	public void recordLoginFailure(LocalDateTime now) {
		this.failedAttempts++;
		if (this.failedAttempts >= MAX_FAILED_ATTEMPTS) {
			this.lockoutUntil = now.plusMinutes(LOCKOUT_MINUTES);
			this.failedAttempts = 0;
		}
	}

	/** 로그인 성공 시 실패 횟수와 잠금을 지웁니다. */
	public void resetLoginFailures() {
		this.failedAttempts = 0;
		this.lockoutUntil = null;
	}

	/** {@code now} 시점에 잠겨 있는지. 잠금 해제 시각이 지났으면 false. */
	public boolean isLocked(LocalDateTime now) {
		return lockoutUntil != null && now.isBefore(lockoutUntil);
	}

	/** 소프트 삭제. 행은 그대로 두고 status 만 DELETED 로 바꿉니다. (ACC-005) */
	public void delete() {
		this.status = EntityStatus.DELETED;
	}
}

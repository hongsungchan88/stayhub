package com.stayhub.account.repository;

import com.stayhub.account.entity.User;
import com.stayhub.common.EntityStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자/직원 계정 DB 접근.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>메서드 이름을 Spring 이 읽고 SQL 을 만들어 줍니다.
 * 예) {@code findByLoginIdAndStatus} → {@code WHERE login_id = ? AND status = ?}
 *
 * <p><b>중복 확인에는 status 조건을 붙이지 않습니다.</b>
 * {@code login_id}·{@code email} 은 DB 에 UNIQUE 제약이 걸려 있어서 삭제된(DELETED) 계정의 값도 다시 쓸 수 없습니다.
 * status 를 붙여 "ACTIVE 중에서만" 확인하면, 삭제된 계정과 같은 아이디로 가입할 때
 * 중복 확인은 통과하고 저장에서 DB 오류가 납니다.
 *
 * <p><b>따라 만들 때 바꿀 곳 (회원가입 담당자용)</b>
 * <br>회원가입은 {@code existsByLoginId}·{@code existsByEmail} 로 중복을 먼저 막고 {@code save} 합니다.
 * 로그인은 {@code findByLoginIdAndStatus(loginId, EntityStatus.ACTIVE)} 로 찾습니다(삭제된 계정은 로그인 불가).
 * 메서드를 새로 추가할 일이 생기면 유형 ①이므로 조회 메서드에는 {@code AndStatus} 를 붙입니다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

	/** 로그인용 조회. 없거나 삭제된 계정이면 빈 Optional. */
	Optional<User> findByLoginIdAndStatus(String loginId, EntityStatus status);

	/** 아이디 중복 확인. 삭제된 계정도 포함합니다. */
	boolean existsByLoginId(String loginId);

	/** 이메일 중복 확인. 삭제된 계정도 포함합니다. */
	boolean existsByEmail(String email);
}

package com.stayhub.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.stayhub.account.entity.User;
import com.stayhub.account.entity.UserRole;
import com.stayhub.account.repository.UserRepository;
import com.stayhub.common.EntityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정 저장·조회와 비밀번호 암호화 테스트.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>실제 DB(application-local.yml 의 MySQL)에 붙어서 확인합니다.
 * {@code @Transactional} 이라 테스트가 끝나면 넣은 데이터가 전부 롤백됩니다.
 *
 * <p><b>따라 만들 때 바꿀 곳 (회원가입 담당자용)</b>
 * <br>회원가입 서비스 테스트({@code UserServiceTest})를 만들 때 {@code saveUser} 도우미와
 * {@code setUp} 의 {@code deleteAll} 을 그대로 가져다 씁니다.
 */
@SpringBootTest
@Transactional
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		// 개발 DB 에 이미 있는 계정과 아이디가 겹치지 않도록 테스트 안에서만 비웁니다.
		// 경고: @Transactional을 지우면 개발 DB의 users 테이블이 실제로 비워집니다. 지우지 마세요.
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("암호화한 비밀번호는 원문과 다르고, matches 로는 일치한다")
	void passwordIsEncoded() {
		String encoded = passwordEncoder.encode("pass1234!");

		assertThat(encoded).isNotEqualTo("pass1234!");
		assertThat(passwordEncoder.matches("pass1234!", encoded)).isTrue();
		assertThat(passwordEncoder.matches("wrong1234!", encoded)).isFalse();
	}

	@Test
	@DisplayName("로그인 아이디로 조회되고, 없는 아이디는 비어 있다")
	void findByLoginId() {
		User saved = saveUser("tester01", "tester01@stayhub.test");

		assertThat(userRepository.findByLoginIdAndStatus("tester01", EntityStatus.ACTIVE))
				.get()
				.satisfies(user -> {
					assertThat(user.getId()).isEqualTo(saved.getId());
					assertThat(user.getRole()).isEqualTo(UserRole.STAFF); // role 을 안 넘기면 STAFF
					assertThat(user.isVerified()).isFalse();
				});
		assertThat(userRepository.findByLoginIdAndStatus("nobody", EntityStatus.ACTIVE)).isEmpty();
	}

	@Test
	@DisplayName("삭제된 계정은 로그인 조회에 안 나오지만, 아이디·이메일 중복으로는 잡힌다")
	void deletedUserStillCountsAsDuplicate() {
		User saved = saveUser("tester01", "tester01@stayhub.test");
		saved.delete();
		userRepository.flush();

		assertThat(userRepository.findByLoginIdAndStatus("tester01", EntityStatus.ACTIVE)).isEmpty();
		assertThat(userRepository.existsByLoginId("tester01")).isTrue();
		assertThat(userRepository.existsByEmail("tester01@stayhub.test")).isTrue();
		assertThat(userRepository.existsByLoginId("nobody")).isFalse();
	}

	private User saveUser(String loginId, String email) {
		return userRepository.save(User.create(
				loginId, passwordEncoder.encode("pass1234!"), "테스터", "010-0000-0000", email, null));
	}
}

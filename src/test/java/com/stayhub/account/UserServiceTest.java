package com.stayhub.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.account.dto.UserCreateRequest;
import com.stayhub.account.dto.UserResponse;
import com.stayhub.account.dto.UserUpdateRequest;
import com.stayhub.account.entity.User;
import com.stayhub.account.entity.UserRole;
import com.stayhub.account.repository.UserRepository;
import com.stayhub.account.service.UserService;
import com.stayhub.common.BusinessException;
import com.stayhub.common.EntityStatus;
import com.stayhub.common.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 직원 계정 관리 서비스 테스트 (ACC-005).
 *
 * <p>{@code @Transactional} 이라 테스트가 끝나면 넣었던 데이터가 전부 되돌려집니다(롤백).
 * 호스트만 부를 수 있는지(403)는 컨트롤러가 확인하므로 여기서는 다루지 않습니다.
 */
@SpringBootTest
@Transactional
class UserServiceTest {

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/** 지금 로그인해 있다고 가정하는 호스트. */
	private User host;

	@BeforeEach
	void setUp() {
		// 개발 DB 에 이미 들어 있는 데이터 때문에 결과가 흔들리지 않도록 테스트 안에서만 비웁니다.
		// @Transactional 이라 테스트가 끝나면 지운 것까지 원래대로 돌아옵니다.
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블이 실제로 비워집니다. 지우지 마세요.
		userRepository.deleteAll();
		host = userRepository.save(User.create(
				"host01", passwordEncoder.encode("host1234!"), "호스트", "010-0000-0000", "host-test@test.com", UserRole.HOST));
	}

	@Test
	@DisplayName("등록하면 목록에 나오고, 미인증·암호화된 비밀번호로 저장된다")
	void createThenAppearsInList() {
		UserResponse created = userService.create(createRequest("front01", "front-test@test.com", UserRole.FRONT));

		PageResponse<UserResponse> all = userService.getList(0);
		assertThat(all.content()).extracting(UserResponse::id).containsExactly(host.getId(), created.id());

		assertThat(created.role()).isEqualTo(UserRole.FRONT);
		assertThat(created.verified()).isFalse(); // 실제로 이메일 인증을 한 게 아니라서 FALSE (총괄 확인)

		User saved = userRepository.findById(created.id()).orElseThrow();
		assertThat(passwordEncoder.matches("staff1234!", saved.getPassword())).isTrue();
	}

	@Test
	@DisplayName("수정하면 바뀐 이름·역할이 조회된다")
	void updateIsApplied() {
		UserResponse created = userService.create(createRequest("front01", "front-test@test.com", UserRole.FRONT));

		userService.update(host.getId(), created.id(), new UserUpdateRequest("청소 담당", UserRole.CLEANING));

		UserResponse found = userService.getDetail(created.id());
		assertThat(found.name()).isEqualTo("청소 담당");
		assertThat(found.role()).isEqualTo(UserRole.CLEANING);
	}

	@Test
	@DisplayName("삭제하면 목록에서 빠지고, 행은 DELETED 로 남는다")
	void deleteRemovesFromList() {
		UserResponse created = userService.create(createRequest("front01", "front-test@test.com", UserRole.FRONT));

		userService.delete(host.getId(), created.id());

		assertThat(userService.getList(0).content()).extracting(UserResponse::id).containsExactly(host.getId());

		// 소프트 삭제라서 DB 에는 행이 그대로 있고 status 만 바뀌었다
		assertThat(userRepository.findById(created.id()))
				.get()
				.extracting(user -> user.getStatus())
				.isEqualTo(EntityStatus.DELETED);
	}

	@Test
	@DisplayName("삭제된 계정을 상세 조회하면 BusinessException 이 난다")
	void getDeletedThrows() {
		UserResponse created = userService.create(createRequest("front01", "front-test@test.com", UserRole.FRONT));
		userService.delete(host.getId(), created.id());

		assertThatThrownBy(() -> userService.getDetail(created.id()))
				.isInstanceOf(BusinessException.class)
				.hasMessage("직원 계정을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("이미 있는 아이디·이메일로 등록하면 BusinessException 이 난다")
	void duplicateLoginIdOrEmailThrows() {
		userService.create(createRequest("front01", "front-test@test.com", UserRole.FRONT));

		assertThatThrownBy(() -> userService.create(createRequest("front01", "other@test.com", UserRole.FRONT)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("이미 사용 중인 아이디입니다.");

		assertThatThrownBy(() -> userService.create(createRequest("front02", "front-test@test.com", UserRole.FRONT)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("이미 사용 중인 이메일입니다.");
	}

	@Test
	@DisplayName("HOST 역할로는 등록·수정할 수 없다")
	void hostRoleNotAllowed() {
		assertThatThrownBy(() -> userService.create(createRequest("host02", "host2-test@test.com", UserRole.HOST)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("직원 역할은 프런트·청소·스태프 중에서 선택해 주세요.");

		UserResponse created = userService.create(createRequest("front01", "front-test@test.com", UserRole.FRONT));
		assertThatThrownBy(() -> userService.update(host.getId(), created.id(), new UserUpdateRequest("프런트", UserRole.HOST)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("직원 역할은 프런트·청소·스태프 중에서 선택해 주세요.");
	}

	@Test
	@DisplayName("본인 계정과 다른 HOST 계정은 수정·삭제할 수 없다")
	void selfAndHostAccountNotEditable() {
		// 본인 계정
		assertThatThrownBy(() -> userService.update(host.getId(), host.getId(), new UserUpdateRequest("호스트", UserRole.FRONT)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("본인 계정은 수정·삭제할 수 없습니다.");
		assertThatThrownBy(() -> userService.delete(host.getId(), host.getId()))
				.isInstanceOf(BusinessException.class)
				.hasMessage("본인 계정은 수정·삭제할 수 없습니다.");

		// 다른 HOST 계정 (화면에서는 만들 수 없어서 리포지토리로 직접 넣음)
		User otherHost = userRepository.save(User.create(
				"host02", passwordEncoder.encode("host1234!"), "호스트2", "010-0000-0001", "host2-test@test.com", UserRole.HOST));
		assertThatThrownBy(() -> userService.update(host.getId(), otherHost.getId(), new UserUpdateRequest("호스트2", UserRole.FRONT)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("호스트 계정은 수정·삭제할 수 없습니다.");
		assertThatThrownBy(() -> userService.delete(host.getId(), otherHost.getId()))
				.isInstanceOf(BusinessException.class)
				.hasMessage("호스트 계정은 수정·삭제할 수 없습니다.");
	}

	/** 테스트용 등록 요청. 비밀번호·이름·전화번호는 고정값입니다. */
	private UserCreateRequest createRequest(String loginId, String email, UserRole role) {
		return new UserCreateRequest(loginId, "staff1234!", "직원", "010-1234-5678", email, role);
	}
}

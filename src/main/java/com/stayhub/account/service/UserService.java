package com.stayhub.account.service;

import com.stayhub.account.dto.UserCreateRequest;
import com.stayhub.account.dto.UserResponse;
import com.stayhub.account.dto.UserUpdateRequest;
import com.stayhub.account.entity.User;
import com.stayhub.account.entity.UserRole;
import com.stayhub.account.repository.UserRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.common.EntityStatus;
import com.stayhub.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 직원 계정 관리 업무 로직 (ACC-005). 가이드북과 같은 유형 ①(소프트 삭제)입니다.
 *
 * <p>컨트롤러에게 요청 DTO 를 받아 엔티티를 만들거나 고치고, 결과를 응답 DTO 로 바꿔 돌려줍니다.
 * 엔티티는 이 클래스 밖으로 나가지 않습니다.
 *
 * <p><b>이 서비스가 맡지 않는 것</b>
 * <ul>
 *   <li>"호스트만 부를 수 있다" — 컨트롤러가 LoginUser 의 role 로 먼저 막습니다(403)</li>
 *   <li>삭제·역할 변경 후 세션 끊기 — 컨트롤러가 LoginSessionManager 로 합니다.
 *       총괄의 LoginService 처럼 서비스는 DB 만 다룹니다</li>
 * </ul>
 *
 * <p><b>총괄 확인 규칙</b>
 * <ul>
 *   <li>등록: 비밀번호 규칙·아이디/이메일 중복 확인은 회원가입과 같게. is_verified 는 FALSE(실제 인증이 아니므로)</li>
 *   <li>역할: FRONT·CLEANING·STAFF 만. HOST 로 만들거나 바꿀 수 없음</li>
 *   <li>수정·삭제: HOST 계정과 호스트 본인 계정은 불가</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class UserService {

	/** 목록 정렬: user_id 오름차순(등록 순). */
	private static final Sort LIST_SORT = Sort.by(
			Sort.Order.asc("id")
	);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 목록. ACTIVE 만, 한 페이지 20건.
	 *
	 * @param page 0부터 시작
	 */
	public PageResponse<UserResponse> getList(int page) {
		Pageable pageable = PageRequest.of(page, PageResponse.DEFAULT_PAGE_SIZE, LIST_SORT);

		Page<User> users = userRepository.findByStatus(EntityStatus.ACTIVE, pageable);

		return PageResponse.from(users.map(UserResponse::from));
	}

	/** 상세. 없거나 삭제된 계정이면 BusinessException. */
	public UserResponse getDetail(Long id) {
		return UserResponse.from(findActive(id));
	}

	/**
	 * 등록. 아이디·이메일 중복이면 BusinessException (삭제된 계정 포함 — UserRepository 주석 참고).
	 * 이메일 인증을 하지 않았으므로 is_verified 는 FALSE 그대로 둡니다(verifyEmail 을 부르지 않음).
	 */
	@Transactional
	public UserResponse create(UserCreateRequest request) {
		checkStaffRole(request.role());
		if (userRepository.existsByLoginId(request.loginId())) {
			throw new BusinessException("이미 사용 중인 아이디입니다.");
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException("이미 사용 중인 이메일입니다.");
		}

		User user = User.create(
				request.loginId(),
				passwordEncoder.encode(request.password()),
				request.name(),
				request.phoneNumber(),
				request.email(),
				request.role()
		);
		return UserResponse.from(userRepository.save(user));
	}

	/**
	 * 수정(이름·역할). 없거나 삭제된 계정, HOST 계정, 본인 계정이면 BusinessException.
	 *
	 * @param loginUserId 지금 로그인한 호스트의 ID. 본인 계정인지 확인하는 데 씁니다
	 */
	@Transactional
	public UserResponse update(Long loginUserId, Long id, UserUpdateRequest request) {
		User user = findActive(id);
		checkEditable(loginUserId, user);
		checkStaffRole(request.role());

		user.update(request.name(), request.role());
		// save() 를 부르지 않아도 트랜잭션이 끝날 때 바뀐 값이 자동으로 DB 에 반영됩니다. (변경 감지)
		// 다만 수정일시(updatedAt)는 반영되는 순간에 채워지므로, 응답에 새 값을 담으려고 먼저 반영합니다.
		userRepository.flush();
		return UserResponse.from(user);
	}

	/**
	 * 소프트 삭제. 행은 남기고 status 만 DELETED 로 바꿉니다(정의서 "로그 보존").
	 * 없거나 이미 삭제된 계정, HOST 계정, 본인 계정이면 BusinessException.
	 */
	@Transactional
	public void delete(Long loginUserId, Long id) {
		User user = findActive(id);
		checkEditable(loginUserId, user);
		user.delete();
	}

	/** 본인 계정과 HOST 계정은 수정·삭제할 수 없습니다(총괄 확인). */
	private void checkEditable(Long loginUserId, User user) {
		if (user.getId().equals(loginUserId)) {
			throw new BusinessException("본인 계정은 수정·삭제할 수 없습니다.");
		}
		if (user.getRole() == UserRole.HOST) {
			throw new BusinessException("호스트 계정은 수정·삭제할 수 없습니다.");
		}
	}

	/** 직원 역할은 FRONT·CLEANING·STAFF 만. (정의서 입력 항목 "역할(프론트/청소/스태프)") */
	private void checkStaffRole(UserRole role) {
		if (role == UserRole.HOST) {
			throw new BusinessException("직원 역할은 프런트·청소·스태프 중에서 선택해 주세요.");
		}
	}

	/** ACTIVE 인 계정 하나를 찾습니다. 없거나 삭제됐으면 예외. */
	private User findActive(Long id) {
		return userRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException("직원 계정을 찾을 수 없습니다."));
	}
}

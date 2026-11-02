package com.stayhub.account.dto;

import com.stayhub.account.entity.User;
import com.stayhub.account.entity.UserRole;
import java.io.Serializable;

/**
 * 세션에 넣어 두는 로그인 사용자 정보. GET /api/auth/me 의 응답이기도 합니다.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>로그인에 성공하면 이 값 하나가 세션의 {@link #SESSION_KEY} 에 들어갑니다.
 * 엔티티({@code User})를 통째로 넣지 않는 이유: 비밀번호 해시까지 세션에 남고,
 * 트랜잭션 밖에서 엔티티를 들고 다니면 값이 DB 와 어긋나도 알 수 없기 때문입니다.
 *
 * <p><b>컨트롤러에서 꺼내 쓰는 법</b>
 * <pre>
 * &#64;GetMapping
 * public ApiResponse&lt;...&gt; list(&#64;SessionAttribute(LoginUser.SESSION_KEY) LoginUser loginUser) {
 *     Long userId = loginUser.userId(); // 예) 담당자 ID 로 저장
 * }
 * </pre>
 * {@code /api/**} 는 인터셉터가 로그인을 먼저 확인하므로, 컨트롤러에서는 이 값이 항상 있습니다.
 *
 * <p>이름·역할이 바뀌어도 세션 값은 다시 로그인할 때까지 그대로입니다(최대 30분).
 */
public record LoginUser(
		Long userId,
		String loginId,
		String name,
		UserRole role
) implements Serializable {

	/** 세션 속성 이름. 이 문자열을 직접 쓰지 말고 상수를 쓰세요. */
	public static final String SESSION_KEY = "LOGIN_USER";

	public static LoginUser from(User user) {
		return new LoginUser(user.getId(), user.getLoginId(), user.getName(), user.getRole());
	}
}

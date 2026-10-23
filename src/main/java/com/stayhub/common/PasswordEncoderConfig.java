package com.stayhub.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 암호화 도구 등록.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>{@link PasswordEncoder}(BCrypt) 를 빈으로 등록합니다. 서비스에서 생성자로 주입받아 씁니다.
 * <ul>
 *   <li>저장할 때 — {@code passwordEncoder.encode(rawPassword)} 결과를 {@code User.create} 에 넘깁니다</li>
 *   <li>로그인할 때 — {@code passwordEncoder.matches(rawPassword, user.getPassword())}</li>
 * </ul>
 * BCrypt 는 같은 비밀번호도 매번 다른 값으로 암호화하므로, 암호화 결과끼리 {@code equals} 로 비교하면 안 됩니다.
 * 반드시 {@code matches} 를 씁니다.
 *
 * <p><b>spring-boot-starter-security 를 쓰지 않는 이유</b>
 * <br>스타터를 넣으면 모든 요청에 기본 로그인 화면이 붙습니다. 지금은 암호화만 필요해서
 * {@code spring-security-crypto} 만 추가했습니다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <br>없습니다. 그대로 주입받아 쓰세요.
 */
@Configuration
public class PasswordEncoderConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}

package com.stayhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * stayhub 애플리케이션 진입점.
 *
 * <p>{@code @EnableJpaAuditing} 은 BaseEntity 의 생성일시·수정일시를
 * 자동으로 채워주기 위해 필요합니다. 빼면 created_at 이 null 로 저장됩니다.
 */
@EnableJpaAuditing
@SpringBootApplication
public class StayhubApplication {

	public static void main(String[] args) {
		SpringApplication.run(StayhubApplication.class, args);
	}
}

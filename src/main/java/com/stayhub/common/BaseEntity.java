package com.stayhub.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모든 엔티티가 상속하는 공통 부모.
 *
 * <p>생성일시·수정일시를 직접 채우지 않아도 JPA 가 자동으로 넣어줍니다.
 * 테이블마다 같은 컬럼을 반복해서 쓰지 않으려고 만들었습니다.
 *
 * <p>{@code @MappedSuperclass} 라서 이 클래스 자체는 테이블이 되지 않고,
 * 상속한 엔티티의 테이블에 아래 두 컬럼이 추가됩니다.
 *
 * <p>사용 예시
 * <pre>
 * &#64;Entity
 * public class Reservation extends BaseEntity {
 *     // 여기에 예약 고유 필드만 작성하면 됩니다.
 * }
 * </pre>
 *
 * <p>동작하려면 {@code StayhubApplication} 에 {@code @EnableJpaAuditing} 이 있어야 합니다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

	/** 최초 저장된 시각. 한 번 저장되면 바뀌지 않습니다. */
	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** 마지막으로 수정된 시각. 수정할 때마다 갱신됩니다. */
	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}

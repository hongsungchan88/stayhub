package com.stayhub.content.repository;

import com.stayhub.common.EntityStatus;
import com.stayhub.content.entity.Guidebook;
import com.stayhub.content.entity.GuidebookCategory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 가이드북 DB 접근.
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>인터페이스만 선언하면 Spring 이 메서드 이름을 읽고 SQL 을 알아서 만들어 줍니다.
 * 예) {@code findByCategoryAndStatus} → {@code WHERE category = ? AND status = ?}
 * 저장({@code save})·ID 조회({@code findById}) 같은 기본 메서드는 {@code JpaRepository} 에 이미 있습니다.
 *
 * <p>모든 조회에 {@code status} 조건이 붙어 있습니다. 삭제된 데이터가 섞여 나오지 않게 하기 위해서입니다.
 * 정렬은 여기서 정하지 않고 서비스가 {@code Pageable} 에 담아 넘깁니다.
 *
 * <p><b>property_id 는 조회 조건에 넣지 않습니다.</b>
 * {@code property_id} 컬럼이 있는 테이블은 등록할 때 {@code PropertyScope} 값을 넣고,
 * 조회 조건에는 넣지 않는다(정책 32행).
 * 내 테이블에 {@code property_id} 가 없으면 관련 줄을 전부 지운다.
 *
 * <p><b>따라 만들 때 바꿀 곳</b>
 * <ul>
 *   <li>인터페이스 이름·제네릭 — {@code XxxRepository extends JpaRepository<Xxx, Long>}</li>
 *   <li>카테고리 필터용 메서드 — 내 테이블에 필터 조건이 없으면 지웁니다</li>
 *   <li>중복 확인 메서드 — 가이드북만의 규칙입니다. 내 테이블에 없으면 지웁니다</li>
 * </ul>
 */
public interface GuidebookRepository extends JpaRepository<Guidebook, Long> { // ★ 바꿀 곳: 이름·엔티티 타입

	/** 목록 (필터 없음). */
	Page<Guidebook> findByStatus(EntityStatus status, Pageable pageable);

	/** 목록 (카테고리 필터). */
	Page<Guidebook> findByCategoryAndStatus( // ★ 바꿀 곳: 내 테이블의 필터 컬럼 (없으면 삭제)
			GuidebookCategory category, EntityStatus status, Pageable pageable);

	/** 상세. 없거나 삭제된 항목이면 빈 Optional. */
	Optional<Guidebook> findByIdAndStatus(Long id, EntityStatus status); // ★ 바꿀 곳: 엔티티 타입

	/** 같은 카테고리에 같은 제목이 있는지. (가이드북만의 규칙 — 다른 테이블엔 보통 없음) */
	boolean existsByCategoryAndTitleAndStatus( // ★ 바꿀 곳: 내 테이블에 중복 규칙이 없으면 삭제
			GuidebookCategory category, String title, EntityStatus status);
}

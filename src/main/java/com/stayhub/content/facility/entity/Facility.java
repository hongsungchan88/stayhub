package com.stayhub.content.facility.entity;

import com.stayhub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 시설 코드 (테이블 {@code facilities}). 유형 ③ — status 가 없고 삭제 기능도 없습니다.
 *
 * <p>주차, 와이파이, 조식 등 사전 정의된 시설 항목 코드입니다.
 * 데이터는 {@code src/main/resources/data.sql} 로 미리 넣고, API 는 조회만 있습니다(총괄 결정).
 *
 * <p><b>가이드북과 다른 점 — PK 가 문자 코드</b>
 * <br>{@code facility_code} 는 AUTO_INCREMENT 번호가 아니라 정해진 코드(예: PARKING)라서
 * {@code @GeneratedValue} 가 없습니다. 필드명은 규칙대로 {@code id} 입니다.
 *
 * <p>{@code created_at} / {@code updated_at} 은 {@link BaseEntity} 상속으로 생깁니다. (정의서에 없지만 허용 — 총괄 결정)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자.
@Entity
@Table(name = "facilities")
public class Facility extends BaseEntity {

	@Id
	@Column(name = "facility_code", length = 20) // 문자 PK — AUTO_INCREMENT 아님
	private String id;

	/** 체크박스 화면 표시명. */
	@Column(name = "facility_name", nullable = false, length = 50)
	private String facilityName;

	@ColumnDefault("0") // 정의서의 "기본 0"
	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	/** 사용 여부. false 면 체크박스 목록에서 제외하고, 숙소에 저장할 수도 없습니다. */
	@ColumnDefault("true") // 정의서의 "기본 TRUE"
	@Column(name = "use_yn", nullable = false, columnDefinition = "BOOLEAN")
	private boolean useYn;

	/**
	 * 시설 코드를 만듭니다.
	 * 등록 API 가 없어서 서비스에서는 쓰지 않고, <b>테스트 데이터 준비용</b>으로만 씁니다.
	 * 실제 데이터는 data.sql 로 넣습니다.
	 */
	public static Facility create(String id, String facilityName, int sortOrder, boolean useYn) {
		Facility facility = new Facility();
		facility.id = id;
		facility.facilityName = facilityName;
		facility.sortOrder = sortOrder;
		facility.useYn = useYn;
		return facility;
	}
}

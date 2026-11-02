package com.stayhub.content.property.entity;

import com.stayhub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 숙소-시설 매핑 (테이블 {@code property_facilities}). 숙소가 보유한 시설을 N:M 으로 연결합니다.
 *
 * <p><b>가이드북과 다른 점 (총괄 결정)</b>
 * <ul>
 *   <li>복합 PK(property_id + facility_code) — {@code @IdClass(PropertyFacilityId.class)}</li>
 *   <li>체크 해제된 시설은 행을 실제로 삭제합니다. 숙소 소개 저장 시 "바뀐 것만" 반영
 *       ({@code PropertyFacilityService.sync})</li>
 * </ul>
 *
 * <p>다른 테이블은 {@code @ManyToOne} 없이 값으로만 들고 있습니다.
 * property_id 에는 등록 시 {@code PropertyScope.DEFAULT_PROPERTY_ID} 를 넣습니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "property_facilities")
@IdClass(PropertyFacilityId.class)
public class PropertyFacility extends BaseEntity {

	/** 숙소 ID. properties.property_id 를 가리킵니다. */
	@Id
	@Column(name = "property_id")
	private Long propertyId;

	/** 시설 코드. facilities.facility_code 를 가리킵니다. */
	@Id
	@Column(name = "facility_code", length = 20)
	private String facilityCode;

	/** 새 매핑을 만듭니다. */
	public static PropertyFacility create(Long propertyId, String facilityCode) {
		PropertyFacility propertyFacility = new PropertyFacility();
		propertyFacility.propertyId = propertyId;
		propertyFacility.facilityCode = facilityCode;
		return propertyFacility;
	}
}

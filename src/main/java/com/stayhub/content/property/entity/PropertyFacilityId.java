package com.stayhub.content.property.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * 숙소-시설 매핑의 복합 PK (property_id + facility_code). {@link PropertyFacility} 의 {@code @IdClass} 입니다.
 *
 * <p>JPA 규칙상 복합 PK 클래스는 아래를 지켜야 합니다.
 * <ul>
 *   <li>{@code Serializable} 구현</li>
 *   <li>엔티티의 {@code @Id} 필드와 <b>이름·타입이 똑같은</b> 필드 (propertyId, facilityCode)</li>
 *   <li>빈 생성자, {@code equals}·{@code hashCode}</li>
 * </ul>
 * Lombok 은 팀 규칙상 @Getter·@NoArgsConstructor 만 쓰므로 equals·hashCode 를 직접 씁니다.
 */
public class PropertyFacilityId implements Serializable {

	private Long propertyId;
	private String facilityCode;

	/** JPA 가 쓰는 빈 생성자. */
	public PropertyFacilityId() {
	}

	public PropertyFacilityId(Long propertyId, String facilityCode) {
		this.propertyId = propertyId;
		this.facilityCode = facilityCode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PropertyFacilityId other)) {
			return false;
		}
		return Objects.equals(propertyId, other.propertyId)
				&& Objects.equals(facilityCode, other.facilityCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(propertyId, facilityCode);
	}
}

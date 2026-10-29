package com.stayhub.content.facility.dto;

import com.stayhub.content.facility.entity.Facility;
import java.time.LocalDateTime;

/**
 * 시설 코드 응답 (목록·상세가 이 모양으로 나갑니다).
 *
 * <p>id 는 시설 코드(예: PARKING)입니다. 변환({@link #from})은 서비스 안에서 호출합니다.
 */
public record FacilityResponse(
		String id,
		String facilityName,
		int sortOrder,
		boolean useYn,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	/** 엔티티를 응답 모양으로 바꿉니다. */
	public static FacilityResponse from(Facility facility) {
		return new FacilityResponse(
				facility.getId(),
				facility.getFacilityName(),
				facility.getSortOrder(),
				facility.isUseYn(),
				facility.getCreatedAt(),
				facility.getUpdatedAt()
		);
	}
}

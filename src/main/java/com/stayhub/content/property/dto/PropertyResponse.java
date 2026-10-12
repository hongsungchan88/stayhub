package com.stayhub.content.property.dto;

import com.stayhub.content.property.entity.Property;
import com.stayhub.content.property.entity.PropertyStatus;
import java.time.LocalDateTime;

/**
 * 숙소 응답 (목록·상세·등록·수정이 모두 이 모양으로 나갑니다).
 *
 * <p>status 는 소프트 삭제용이 아닌 업무 상태(DRAFT·PUBLISHED)라서 응답에 넣습니다.
 * 변환({@link #from})은 서비스 안에서 호출합니다. 컨트롤러는 엔티티를 만지지 않습니다.
 */
public record PropertyResponse(
		Long id,
		String propertyName,
		String description,
		PropertyStatus status,
		Long updatedBy,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	/** 엔티티를 응답 모양으로 바꿉니다. */
	public static PropertyResponse from(Property property) {
		return new PropertyResponse(
				property.getId(),
				property.getPropertyName(),
				property.getDescription(),
				property.getStatus(),
				property.getUpdatedBy(),
				property.getCreatedAt(),
				property.getUpdatedAt()
		);
	}
}

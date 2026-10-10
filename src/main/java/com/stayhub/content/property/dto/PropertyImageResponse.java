package com.stayhub.content.property.dto;

import com.stayhub.content.property.entity.PropertyImage;
import java.time.LocalDateTime;

/**
 * 숙소 이미지 응답 (목록·상세·등록·수정이 모두 이 모양으로 나갑니다).
 *
 * <p>propertyId 는 내부용 값이라 뺍니다. 정의서에 updated_at 이 없으므로 updatedAt 도 없습니다.
 */
public record PropertyImageResponse(
		Long id,
		String filePath,
		int fileSize,
		String fileExt,
		boolean isMain,
		int sortOrder,
		LocalDateTime createdAt
) {

	/** 엔티티를 응답 모양으로 바꿉니다. */
	public static PropertyImageResponse from(PropertyImage propertyImage) {
		return new PropertyImageResponse(
				propertyImage.getId(),
				propertyImage.getFilePath(),
				propertyImage.getFileSize(),
				propertyImage.getFileExt(),
				propertyImage.isMain(),
				propertyImage.getSortOrder(),
				propertyImage.getCreatedAt()
		);
	}
}

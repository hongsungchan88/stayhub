package com.stayhub.content.property.dto;

import com.stayhub.content.property.entity.PropertyImage;
import java.time.LocalDateTime;

/**
 * 숙소 이미지 응답 (목록·상세·등록·수정이 모두 이 모양으로 나갑니다).
 *
 * <p>propertyId 는 내부용 값이라 응답에서 뺍니다.
 * 테이블에 {@code updated_at} 컬럼이 없어서 수정일시도 넣지 않습니다.
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
	public static PropertyImageResponse from(PropertyImage image) {
		return new PropertyImageResponse(
				image.getId(),
				image.getFilePath(),
				image.getFileSize(),
				image.getFileExt(),
				image.isMain(),
				image.getSortOrder(),
				image.getCreatedAt()
		);
	}
}

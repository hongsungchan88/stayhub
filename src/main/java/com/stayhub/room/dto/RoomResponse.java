package com.stayhub.room.dto;

import com.stayhub.room.entity.Room;
import com.stayhub.room.entity.RoomStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 객실 응답 (목록·상세·등록·수정이 모두 이 모양으로 나갑니다).
 *
 * <p>propertyId 는 내부용이라 뺍니다. 객실 상태(status)는 업무 상태라서 넣습니다.
 * 변환({@link #from})은 서비스 안에서 호출합니다. 컨트롤러는 엔티티를 만지지 않습니다.
 */
public record RoomResponse(
		Long id,
		String roomNumber,
		String roomType,
		BigDecimal basePrice,
		RoomStatus status,
		Long statusUpdatedBy,
		LocalDateTime statusUpdatedAt,
		int maxOccupancy,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	/** 엔티티를 응답 모양으로 바꿉니다. */
	public static RoomResponse from(Room room) {
		return new RoomResponse(
				room.getId(),
				room.getRoomNumber(),
				room.getRoomType(),
				room.getBasePrice(),
				room.getStatus(),
				room.getStatusUpdatedBy(),
				room.getStatusUpdatedAt(),
				room.getMaxOccupancy(),
				room.getCreatedAt(),
				room.getUpdatedAt()
		);
	}
}

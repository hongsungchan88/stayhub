package com.stayhub.room.dto;

import com.stayhub.room.entity.RoomStatus;
import com.stayhub.room.entity.RoomStatusLog;
import java.time.LocalDateTime;

/**
 * 객실 상태 변경 이력 응답 (목록·상세·등록·수정이 모두 이 모양으로 나갑니다).
 *
 * <p>roomId·changedBy 는 화면에 필요한 참조 ID 라서 넣습니다.
 * 변환({@link #from})은 서비스 안에서 호출합니다. 컨트롤러는 엔티티를 만지지 않습니다.
 */
public record RoomStatusLogResponse(
		Long id,
		Long roomId,
		RoomStatus previousStatus,
		RoomStatus newStatus,
		String changeReason,
		Long changedBy,
		boolean isConflict,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	/** 엔티티를 응답 모양으로 바꿉니다. */
	public static RoomStatusLogResponse from(RoomStatusLog roomStatusLog) {
		return new RoomStatusLogResponse(
				roomStatusLog.getId(),
				roomStatusLog.getRoomId(),
				roomStatusLog.getPreviousStatus(),
				roomStatusLog.getNewStatus(),
				roomStatusLog.getChangeReason(),
				roomStatusLog.getChangedBy(),
				roomStatusLog.isConflict(),
				roomStatusLog.getCreatedAt(),
				roomStatusLog.getUpdatedAt()
		);
	}
}

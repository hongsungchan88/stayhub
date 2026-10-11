package com.stayhub.room.entity;

import com.stayhub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 객실 상태 변경 이력 (테이블 {@code room_status_logs}).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>객실 상태 변경 이벤트가 생길 때마다의 변경 이력과 동시 변경 충돌 여부를 담습니다. (ROM-001)
 * {@code created_at} / {@code updated_at} 은 {@link BaseEntity} 를 상속해서 자동으로 채워집니다.
 *
 * <p>값을 바꾸는 방법은 아래 메서드뿐입니다.
 * <ul>
 *   <li>새로 만들 때 — {@link #create}</li>
 *   <li>고칠 때 — {@link #update}</li>
 * </ul>
 * 삭제 기능은 없습니다.
 *
 * <p><b>필드명 규칙</b> — 자기 PK 는 {@code id}, 다른 테이블을 가리키는 값은 {@code xxxId}.
 * 예) {@code roomId}(rooms), {@code changedBy}(users)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "room_status_logs")
public class RoomStatusLog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "status_log_id")
	private Long id;

	/** 대상 객실 ID. rooms 테이블을 가리킵니다. {@code @ManyToOne} 을 쓰지 않고 값(Long)만 들고 있습니다. */
	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Enumerated(EnumType.STRING)
	@Column(name = "previous_status", nullable = false, columnDefinition = "VARCHAR(20)")
	private RoomStatus previousStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "new_status", nullable = false, columnDefinition = "VARCHAR(20)")
	private RoomStatus newStatus;

	/** 예약/체크인/체크아웃/청소 완료 등. 정의서에 값 목록이 정해져 있지 않아 enum 이 아닌 문자열로 둡니다. */
	@Column(name = "change_reason", nullable = false, length = 30)
	private String changeReason;

	/** 상태 변경 담당자 ID. users 테이블을 가리킵니다. */
	@Column(name = "changed_by", nullable = false)
	private Long changedBy;

	/** 동시 상태 변경 충돌 여부. 충돌 시 TRUE (반려 처리). */
	@ColumnDefault("false") // 정의서의 "기본 FALSE"
	@Column(name = "is_conflict", nullable = false)
	private boolean isConflict;

	/**
	 * 새 상태 변경 이력을 만듭니다.
	 *
	 * @param isConflict 비어 있으면 false
	 */
	public static RoomStatusLog create(
			Long roomId,
			RoomStatus previousStatus,
			RoomStatus newStatus,
			String changeReason,
			Long changedBy,
			Boolean isConflict
	) {
		RoomStatusLog roomStatusLog = new RoomStatusLog();
		roomStatusLog.roomId = roomId;
		roomStatusLog.previousStatus = previousStatus;
		roomStatusLog.newStatus = newStatus;
		roomStatusLog.changeReason = changeReason;
		roomStatusLog.changedBy = changedBy;
		roomStatusLog.isConflict = defaultIsConflict(isConflict);
		return roomStatusLog;
	}

	/** 내용을 고칩니다. */
	public void update(
			Long roomId,
			RoomStatus previousStatus,
			RoomStatus newStatus,
			String changeReason,
			Long changedBy,
			Boolean isConflict
	) {
		this.roomId = roomId;
		this.previousStatus = previousStatus;
		this.newStatus = newStatus;
		this.changeReason = changeReason;
		this.changedBy = changedBy;
		this.isConflict = defaultIsConflict(isConflict);
	}

	/** 정의서의 "기본 FALSE" — 비어 있으면 false 를 넣습니다. */
	private static boolean defaultIsConflict(Boolean isConflict) {
		return isConflict != null && isConflict;
	}
}

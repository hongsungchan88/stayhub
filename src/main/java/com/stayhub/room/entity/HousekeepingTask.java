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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 청소/점검 작업 스케줄 (테이블 {@code housekeeping_tasks}).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>체크아웃 완료 객실 기준으로 스태프별 청소·점검 작업을 담습니다. (ROM-003)
 * {@code created_at} / {@code updated_at} 은 {@link BaseEntity} 를 상속해서 자동으로 채워집니다.
 *
 * <p>값을 바꾸는 방법은 아래 메서드뿐입니다.
 * <ul>
 *   <li>새로 만들 때 — {@link #create}</li>
 *   <li>고칠 때 — {@link #update}</li>
 * </ul>
 * 유형 ②(업무 상태 있음) 테이블이라 삭제 기능은 없습니다.
 *
 * <p><b>필드명 규칙</b> — 자기 PK 는 {@code id}, 다른 테이블을 가리키는 값은 {@code xxxId}.
 * 예) {@code roomId}(rooms), {@code assignedStaffId}(users)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 쓰는 빈 생성자. 직접 new 하지 말고 create() 를 쓰세요.
@Entity
@Table(name = "housekeeping_tasks")
public class HousekeepingTask extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
	@Column(name = "task_id")
	private Long id;

	/** 대상 객실 ID. rooms 테이블을 가리킵니다. {@code @ManyToOne} 을 쓰지 않고 값(Long)만 들고 있습니다. */
	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Enumerated(EnumType.STRING)
	@ColumnDefault("'CLEANING'") // 정의서의 기본값. 문자열이라 작은따옴표로 한 번 더 감쌉니다.
	@Column(name = "task_type", nullable = false, columnDefinition = "VARCHAR(20)")
	private HousekeepingTaskType taskType;

	/** 체크아웃 완료 시각. 이 시각 기준으로 작업을 배정합니다. */
	@Column(name = "checkout_time", nullable = false)
	private LocalDateTime checkoutTime;

	/** 배정된 청소 스태프 ID. users 테이블을 가리킵니다. 아직 배정 전이면 null. */
	@Column(name = "assigned_staff_id")
	private Long assignedStaffId;

	@Enumerated(EnumType.STRING)
	@ColumnDefault("'SCHEDULED'") // 정의서의 기본값. 문자열이라 작은따옴표로 한 번 더 감쌉니다.
	@Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20)")
	private HousekeepingTaskStatus status;

	/** 재배정 여부. 스태프 부족·일정 중복으로 수동 재배정했으면 TRUE. */
	@ColumnDefault("false") // 정의서의 "기본 FALSE"
	@Column(name = "is_reassigned", nullable = false)
	private boolean isReassigned;

	/** 작업 배정(스케줄) 일시. */
	@Column(name = "scheduled_at", nullable = false)
	private LocalDateTime scheduledAt;

	/** 작업 완료 일시. 아직 완료 전이면 null. */
	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	/**
	 * 새 작업을 만듭니다. 상태는 항상 SCHEDULED 로 시작합니다.
	 *
	 * @param taskType     비어 있으면 CLEANING
	 * @param isReassigned 비어 있으면 false
	 */
	public static HousekeepingTask create(
			Long roomId,
			HousekeepingTaskType taskType,
			LocalDateTime checkoutTime,
			Long assignedStaffId,
			Boolean isReassigned,
			LocalDateTime scheduledAt
	) {
		HousekeepingTask housekeepingTask = new HousekeepingTask();
		housekeepingTask.roomId = roomId;
		housekeepingTask.taskType = defaultTaskType(taskType);
		housekeepingTask.checkoutTime = checkoutTime;
		housekeepingTask.assignedStaffId = assignedStaffId;
		housekeepingTask.isReassigned = defaultIsReassigned(isReassigned);
		housekeepingTask.scheduledAt = scheduledAt;
		housekeepingTask.status = HousekeepingTaskStatus.SCHEDULED;
		return housekeepingTask;
	}

	/** 내용을 고칩니다. status 와 completedAt 은 여기서 바꾸지 않습니다. */
	public void update(
			Long roomId,
			HousekeepingTaskType taskType,
			LocalDateTime checkoutTime,
			Long assignedStaffId,
			Boolean isReassigned,
			LocalDateTime scheduledAt
	) {
		this.roomId = roomId;
		this.taskType = defaultTaskType(taskType);
		this.checkoutTime = checkoutTime;
		this.assignedStaffId = assignedStaffId;
		this.isReassigned = defaultIsReassigned(isReassigned);
		this.scheduledAt = scheduledAt;
	}

	/** 정의서의 기본값 "CLEANING" — 비어 있으면 CLEANING 을 넣습니다. */
	private static HousekeepingTaskType defaultTaskType(HousekeepingTaskType taskType) {
		return taskType == null ? HousekeepingTaskType.CLEANING : taskType;
	}

	/** 정의서의 "기본 FALSE" — 비어 있으면 false 를 넣습니다. */
	private static boolean defaultIsReassigned(Boolean isReassigned) {
		return isReassigned != null && isReassigned;
	}
}

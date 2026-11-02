/**
 * 객실 도메인의 entity 패키지.
 *
 * <p>DB 테이블과 1:1로 대응하는 JPA 엔티티를 두는 곳입니다. BaseEntity 를 상속하세요.
 *
 * <p>들어 있는 파일
 * <ul>
 *   <li>{@code Room} — rooms 테이블 한 행</li>
 *   <li>{@code RoomStatus} — status 컬럼의 값 목록 (room_status_logs 의 previous_status·new_status 도 같이 씀)</li>
 *   <li>{@code RoomStatusLog} — room_status_logs 테이블 한 행</li>
 *   <li>{@code HousekeepingTask} — housekeeping_tasks 테이블 한 행</li>
 *   <li>{@code HousekeepingTaskStatus} — 작업 상태 값 목록</li>
 *   <li>{@code HousekeepingTaskType} — 작업 유형 값 목록</li>
 *   <li>{@code PriceRule} — price_rules 테이블 한 행 (v2 이관, 스키마만 유지)</li>
 *   <li>{@code PriceRuleAdjustType} — 가격 조정 방식 값 목록 (v2 이관)</li>
 *   <li>{@code PriceRuleType} — 가격 규칙 유형 값 목록 (v2 이관)</li>
 *   <li>{@code DailyRoomRate} — daily_room_rates 테이블 한 행 (v2 이관, 스키마만 유지)</li>
 * </ul>
 */
package com.stayhub.room.entity;

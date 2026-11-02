/**
 * 객실 도메인의 dto 패키지.
 *
 * <p>요청·응답용 데이터 전달 객체를 두는 곳입니다. 엔티티를 그대로 노출하지 않기 위해 씁니다.
 *
 * <p>들어 있는 파일
 * <ul>
 *   <li>{@code RoomCreateRequest} — 등록 요청</li>
 *   <li>{@code RoomUpdateRequest} — 수정 요청</li>
 *   <li>{@code RoomResponse} — 목록·상세·등록·수정·상태 변경 응답</li>
 *   <li>{@code RoomStatusChangeRequest} — 객실 상태 변경 요청 (ROM-001)</li>
 *   <li>{@code RoomStatusLogResponse} — 상태 변경 이력 목록·상세 응답</li>
 *   <li>{@code HousekeepingTaskCreateRequest} — 청소/점검 작업 등록 요청</li>
 *   <li>{@code HousekeepingTaskUpdateRequest} — 청소/점검 작업 수정 요청</li>
 *   <li>{@code HousekeepingTaskResponse} — 청소/점검 작업 목록·상세·등록·수정 응답</li>
 * </ul>
 */
package com.stayhub.room.dto;

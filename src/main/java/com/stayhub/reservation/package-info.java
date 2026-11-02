/**
 * 예약 도메인.
 *
 * <p>담당: 팀원 B · 브랜치 코드: {@code rsv}
 *
 * <p>이 폴더 안에서만 작업하세요. 다른 도메인 폴더는 건드리지 않습니다.
 * 하위 패키지는 controller / service / repository / entity / dto 로 나뉩니다.
 *
 * <p><b>총괄이 먼저 올린 틀</b> (정책정의서 39행 — 고치기 전에 총괄과 상의)
 * <ul>
 *   <li>{@code entity/Reservation} — 예약 엔티티 (@Version)</li>
 *   <li>{@code entity/ReservationStatus} — 예약 상태와 상태 전이 규칙 ({@code canChangeTo})</li>
 *   <li>{@code repository/ReservationRepository} — 날짜 겹침 확인</li>
 *   <li>{@code service/ReservationService} — 예약 생성(중복 방지·동시 예약 차단)과 상태 변경</li>
 * </ul>
 * 예약 생성은 반드시 {@code ReservationService.create} 를 거칩니다.
 * 컨트롤러·DTO·목록·검색·취소 수수료·체크인/아웃 연동은 담당자가 이 위에 만듭니다.
 */
package com.stayhub.reservation;

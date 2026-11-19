package com.stayhub.statistics.dto;

/**
 * 데이터 내보내기 항목 (STA-004 입력 "조회 조건(기간/항목)").
 * 대시보드에 보이는 표 하나 = 값 하나입니다.
 */
public enum ExportType {

	/** 객실 가동률 (STA-001) */
	OCCUPANCY,

	/** 채널별 예약·매출 (STA-002) */
	CHANNEL,

	/** 매출 (STA-003) */
	REVENUE
}

package com.stayhub.statistics.controller;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import com.stayhub.common.ApiResponse;
import com.stayhub.common.ForbiddenException;
import com.stayhub.statistics.dto.ChannelStatisticsResponse;
import com.stayhub.statistics.dto.ExportType;
import com.stayhub.statistics.dto.OccupancyResponse;
import com.stayhub.statistics.dto.RevenueResponse;
import com.stayhub.statistics.dto.StatisticsUnit;
import com.stayhub.statistics.service.StatisticsService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

/**
 * 통계·대시보드 API (STA-001~004). 호스트·관리 스태프(STAFF)만 부를 수 있습니다(총괄 결정 7).
 *
 * <pre>
 * GET /api/statistics/occupancy?from=2026-11-01&amp;to=2026-11-30&amp;unit=DAY   객실 가동률 (unit: DAY·WEEK·MONTH, 생략 시 DAY)
 * GET /api/statistics/channels?from=...&amp;to=...&amp;channelId=1             채널별 예약 건수·매출 (channelId 생략 시 전체)
 * GET /api/statistics/revenue?from=...&amp;to=...&amp;unit=MONTH               매출 합계·전월 대비 증감률
 * GET /api/statistics/export?type=REVENUE&amp;from=...&amp;to=...&amp;unit=DAY      CSV 다운로드 (type: OCCUPANCY·CHANNEL·REVENUE)
 * </pre>
 * 날짜는 {@code 2026-11-01} 형식이고 from·to 둘 다 포함입니다. 조회 기간은 1년까지.
 *
 * <p><b>역할 확인</b> — 모든 메서드 첫 줄에서 {@link #requireStatisticsRole} 를 부릅니다.
 * 로그인 여부는 인터셉터가 먼저 확인합니다(아니면 401).
 *
 * <p><b>export 는 ApiResponse 가 아니라 파일로 응답합니다.</b> 브라우저가 바로 내려받도록
 * {@code text/csv} + {@code Content-Disposition: attachment} 를 붙입니다. 실패(400·403)는 다른 API 와 같은 JSON 입니다.
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

	private final StatisticsService statisticsService;

	public StatisticsController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping("/occupancy")
	public ApiResponse<OccupancyResponse> getOccupancy(
			@SessionAttribute(LoginUser.SESSION_KEY) LoginUser loginUser,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "DAY") StatisticsUnit unit
	) {
		requireStatisticsRole(loginUser);
		return ApiResponse.success(statisticsService.getOccupancy(from, to, unit));
	}

	@GetMapping("/channels")
	public ApiResponse<ChannelStatisticsResponse> getChannels(
			@SessionAttribute(LoginUser.SESSION_KEY) LoginUser loginUser,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long channelId
	) {
		requireStatisticsRole(loginUser);
		return ApiResponse.success(statisticsService.getChannels(from, to, channelId));
	}

	@GetMapping("/revenue")
	public ApiResponse<RevenueResponse> getRevenue(
			@SessionAttribute(LoginUser.SESSION_KEY) LoginUser loginUser,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "DAY") StatisticsUnit unit
	) {
		requireStatisticsRole(loginUser);
		return ApiResponse.success(statisticsService.getRevenue(from, to, unit));
	}

	@GetMapping("/export")
	public ResponseEntity<byte[]> export(
			@SessionAttribute(LoginUser.SESSION_KEY) LoginUser loginUser,
			@RequestParam ExportType type,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "DAY") StatisticsUnit unit
	) {
		requireStatisticsRole(loginUser);
		byte[] body = statisticsService.exportCsv(type, from, to, unit).getBytes(StandardCharsets.UTF_8);

		// 예) statistics-revenue-2026-11-01-2026-11-30.csv
		String fileName = "statistics-" + type.name().toLowerCase(Locale.ROOT) + "-" + from + "-" + to + ".csv";
		return ResponseEntity.ok()
				.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
				.body(body);
	}

	/** 호스트·관리 스태프(STAFF)가 아니면 403 "권한이 없습니다." (총괄 결정 7) */
	private void requireStatisticsRole(LoginUser loginUser) {
		if (loginUser.role() != UserRole.HOST && loginUser.role() != UserRole.STAFF) {
			throw new ForbiddenException("권한이 없습니다.");
		}
	}
}

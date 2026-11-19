package com.stayhub.statistics.service;

import com.stayhub.channel.entity.Channel;
import com.stayhub.common.BusinessException;
import com.stayhub.payment.entity.Payment;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.entity.ReservationStatus;
import com.stayhub.statistics.dto.ChannelStatisticsResponse;
import com.stayhub.statistics.dto.ExportType;
import com.stayhub.statistics.dto.OccupancyResponse;
import com.stayhub.statistics.dto.RevenueResponse;
import com.stayhub.statistics.dto.StatisticsUnit;
import com.stayhub.statistics.repository.StatisticsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통계 계산 (STA-001~004). 저장하지 않고 조회할 때마다 계산합니다(총괄 결정).
 *
 * <p><b>이 파일이 하는 일</b>
 * <ul>
 *   <li>{@link #getOccupancy} — 객실 가동률 (STA-001)</li>
 *   <li>{@link #getChannels} — 채널별 예약 건수·매출 (STA-002)</li>
 *   <li>{@link #getRevenue} — 매출 합계·전월 대비 증감률 (STA-003)</li>
 *   <li>{@link #exportCsv} — 위 결과를 CSV 로 (STA-004)</li>
 * </ul>
 * 금액·비율은 전부 BigDecimal, 비율은 소수 둘째 자리 반올림입니다.
 * 호스트·관리 스태프만 부를 수 있는지는 컨트롤러가 확인합니다.
 *
 * <p>쓰기가 없어서 클래스의 {@code @Transactional(readOnly = true)} 만 있습니다.
 */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

	/** 판매로 세는 예약 상태 (총괄 결정 8). NOSHOW·CANCELLED 는 객실을 다시 내준 것이라 뺍니다. */
	static final Set<ReservationStatus> SOLD_STATUSES =
			EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN, ReservationStatus.CHECKED_OUT);

	/** 채널 없는 예약(channel_id NULL)을 묶는 채널. data.sql 의 1번 "직접 예약" (총괄 결정 11). */
	static final Long DIRECT_CHANNEL_ID = 1L;

	/** 한 번에 조회할 수 있는 최대 일수. 매번 계산하므로 너무 긴 기간은 막습니다. */
	static final long MAX_DAYS = 366;

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private final StatisticsRepository statisticsRepository;

	public StatisticsService(StatisticsRepository statisticsRepository) {
		this.statisticsRepository = statisticsRepository;
	}

	/**
	 * 객실 가동률 (STA-001).
	 * 날짜마다 그날 투숙 중인 예약의 객실을 중복 없이 세고, 구간별로 합해 가동률을 냅니다.
	 */
	public OccupancyResponse getOccupancy(LocalDate from, LocalDate to, StatisticsUnit unit) {
		validatePeriod(from, to);
		long totalRooms = statisticsRepository.countRooms();
		List<Reservation> stays = statisticsRepository.findStays(from, to, SOLD_STATUSES);

		// 날짜 → 그날 판매된 객실 ID (Set 이라 같은 객실은 한 번만 셉니다)
		Map<LocalDate, Set<Long>> soldRoomsByDate = new HashMap<>();
		for (Reservation stay : stays) {
			LocalDate night = stay.getCheckInDate().isBefore(from) ? from : stay.getCheckInDate();
			while (night.isBefore(stay.getCheckOutDate()) && !night.isAfter(to)) {
				soldRoomsByDate.computeIfAbsent(night, date -> new HashSet<>()).add(stay.getRoomId());
				night = night.plusDays(1);
			}
		}

		List<OccupancyResponse.Item> items = new ArrayList<>();
		for (Period period : periods(from, to, unit)) {
			long sold = 0;
			for (LocalDate date = period.start(); !date.isAfter(period.end()); date = date.plusDays(1)) {
				sold += soldRoomsByDate.getOrDefault(date, Set.of()).size();
			}
			long available = totalRooms * period.days();
			items.add(new OccupancyResponse.Item(period.start(), period.end(), sold, available, percent(sold, available)));
		}
		return new OccupancyResponse(totalRooms, !stays.isEmpty() && totalRooms > 0, items);
	}

	/**
	 * 채널별 예약 건수·매출 (STA-002).
	 *
	 * @param channelId 비어 있으면 전체 채널. 지정하면 그 채널만
	 */
	public ChannelStatisticsResponse getChannels(LocalDate from, LocalDate to, Long channelId) {
		validatePeriod(from, to);
		LocalDateTime start = from.atStartOfDay();
		LocalDateTime end = to.plusDays(1).atStartOfDay();

		Map<Long, Long> bookingCounts = new HashMap<>();
		for (Reservation reservation : statisticsRepository.findCreatedBetween(start, end, ReservationStatus.CANCELLED)) {
			Long key = reservation.getChannelId() == null ? DIRECT_CHANNEL_ID : reservation.getChannelId();
			bookingCounts.merge(key, 1L, Long::sum);
		}

		Map<Long, BigDecimal> sales = new HashMap<>();
		for (Payment payment : statisticsRepository.findPaymentsBetween(start, end)) {
			sales.merge(payment.getChannelId(), revenueOf(payment), BigDecimal::add);
		}

		// 채널 이름표 순서대로, 건수·매출이 0 인 채널도 포함합니다. 이름표에 없는 ID 는 "채널 #ID" 로 뒤에 붙입니다.
		Map<Long, String> names = new LinkedHashMap<>();
		for (Channel channel : statisticsRepository.findAllChannels()) {
			names.put(channel.getId(), channel.getChannelName());
		}
		for (Long id : bookingCounts.keySet()) {
			names.putIfAbsent(id, "채널 #" + id);
		}
		for (Long id : sales.keySet()) {
			names.putIfAbsent(id, "채널 #" + id);
		}

		if (channelId != null && !names.containsKey(channelId)) {
			throw new BusinessException("채널을 찾을 수 없습니다.");
		}

		List<ChannelStatisticsResponse.Item> items = new ArrayList<>();
		names.forEach((id, name) -> {
			if (channelId == null || channelId.equals(id)) {
				items.add(new ChannelStatisticsResponse.Item(
						id, name, bookingCounts.getOrDefault(id, 0L), money(sales.getOrDefault(id, BigDecimal.ZERO))));
			}
		});
		return new ChannelStatisticsResponse(items);
	}

	/** 매출 합계·전월 대비 증감률 (STA-003). */
	public RevenueResponse getRevenue(LocalDate from, LocalDate to, StatisticsUnit unit) {
		validatePeriod(from, to);
		List<Payment> payments = statisticsRepository.findPaymentsBetween(
				from.atStartOfDay(), to.plusDays(1).atStartOfDay());

		List<RevenueResponse.Item> items = new ArrayList<>();
		BigDecimal total = BigDecimal.ZERO;
		for (Period period : periods(from, to, unit)) {
			BigDecimal sum = BigDecimal.ZERO;
			for (Payment payment : payments) {
				LocalDate paidDate = payment.getCreatedAt().toLocalDate();
				if (!paidDate.isBefore(period.start()) && !paidDate.isAfter(period.end())) {
					sum = sum.add(revenueOf(payment));
				}
			}
			items.add(new RevenueResponse.Item(period.start(), period.end(), money(sum)));
			total = total.add(sum);
		}

		// 비교 기간: 조회 기간을 한 달 앞으로 옮긴 기간 (총괄 결정 5)
		LocalDate previousFrom = from.minusMonths(1);
		LocalDate previousTo = previousEndDate(to);
		BigDecimal previous = BigDecimal.ZERO;
		for (Payment payment : statisticsRepository.findPaymentsBetween(
				previousFrom.atStartOfDay(), previousTo.plusDays(1).atStartOfDay())) {
			previous = previous.add(revenueOf(payment));
		}

		return new RevenueResponse(
				money(total), previousFrom, previousTo, money(previous), growthRate(total, previous), items);
	}

	/**
	 * CSV 내보내기 (STA-004). 맨 앞에 UTF-8 BOM 을 붙여 엑셀에서 한글이 깨지지 않게 합니다(총괄 결정 6).
	 * 데이터가 없으면 머리글만 있는 파일이 됩니다(다운로드 버튼 비활성화는 화면이 맡습니다).
	 */
	public String exportCsv(ExportType type, LocalDate from, LocalDate to, StatisticsUnit unit) {
		StringBuilder csv = new StringBuilder("\uFEFF");
		switch (type) {
			case OCCUPANCY -> {
				OccupancyResponse response = getOccupancy(from, to, unit);
				line(csv, "시작일", "종료일", "판매 객실·일", "판매 가능 객실·일", "가동률(%)");
				for (OccupancyResponse.Item item : response.items()) {
					line(csv, item.startDate().toString(), item.endDate().toString(),
							String.valueOf(item.soldRoomNights()), String.valueOf(item.availableRoomNights()),
							item.occupancyRate().toPlainString());
				}
			}
			case CHANNEL -> {
				ChannelStatisticsResponse response = getChannels(from, to, null);
				line(csv, "채널", "예약 건수", "매출");
				for (ChannelStatisticsResponse.Item item : response.items()) {
					line(csv, item.channelName(), String.valueOf(item.bookingCount()), item.salesAmount().toPlainString());
				}
			}
			case REVENUE -> {
				RevenueResponse response = getRevenue(from, to, unit);
				line(csv, "시작일", "종료일", "매출");
				for (RevenueResponse.Item item : response.items()) {
					line(csv, item.startDate().toString(), item.endDate().toString(), item.revenue().toPlainString());
				}
				line(csv, "합계", "", response.totalRevenue().toPlainString());
				line(csv, "전월 같은 기간", response.previousStartDate() + " ~ " + response.previousEndDate(),
						response.previousRevenue().toPlainString());
				line(csv, "전월 대비 증감률(%)", "",
						response.growthRate() == null ? "비교 불가" : response.growthRate().toPlainString());
			}
		}
		return csv.toString();
	}

	/**
	 * 결제 1건이 매출에 들어가는 금액 (총괄 결정 3).
	 * <ul>
	 *   <li>COMPLETED·REFUND_REQUESTED — net_revenue 전부</li>
	 *   <li>PARTIALLY_REFUNDED — net_revenue × (1 − refund_amount ÷ payment_amount)</li>
	 *   <li>REFUNDED·FAILED — 0</li>
	 * </ul>
	 */
	public static BigDecimal revenueOf(Payment payment) {
		return switch (payment.getPaymentStatus()) {
			case COMPLETED, REFUND_REQUESTED -> payment.getNetRevenue();
			case PARTIALLY_REFUNDED -> {
				BigDecimal paid = payment.getPaymentAmount();
				BigDecimal refunded = payment.getRefundAmount() == null ? BigDecimal.ZERO : payment.getRefundAmount();
				if (paid == null || paid.signum() == 0) {
					yield BigDecimal.ZERO;
				}
				// net × (1 − refund ÷ paid) = net × (paid − refund) ÷ paid. 나눗셈을 마지막에 해서 반올림 오차를 줄입니다.
				yield payment.getNetRevenue().multiply(paid.subtract(refunded)).divide(paid, 2, RoundingMode.HALF_UP);
			}
			case REFUNDED, FAILED -> BigDecimal.ZERO;
		};
	}

	/** (이번 − 비교) ÷ 비교 × 100, 소수 둘째 자리 반올림. 비교가 0 이면 null — 화면은 "비교 불가" (총괄 결정 5). */
	public static BigDecimal growthRate(BigDecimal current, BigDecimal previous) {
		if (previous.signum() == 0) {
			return null;
		}
		return current.subtract(previous).multiply(HUNDRED).divide(previous, 2, RoundingMode.HALF_UP);
	}

	/**
	 * 비교 기간의 종료일 = to 의 한 달 전.
	 * 단, to가 그 달의 마지막 날이면 지난달의 마지막 날로 맞춥니다.
	 * 예) 11/30 -> 10/31 (그냥 한 달 빼면 10/30 이라 10/31 하루가 빠짐), 3/31 -> 2/28
	 */
	public static LocalDate previousEndDate(LocalDate to) {
		LocalDate previousTo = to.minusMonths(1);
		if (to.equals(to.with(TemporalAdjusters.lastDayOfMonth()))) {
			previousTo = previousTo.with(TemporalAdjusters.lastDayOfMonth());
		}
		return previousTo;
	}

	/** 조회 기간을 단위별 구간으로 자릅니다. 첫·마지막 구간은 조회 기간에 맞춰 잘립니다. */
	public static List<Period> periods(LocalDate from, LocalDate to, StatisticsUnit unit) {
		List<Period> periods = new ArrayList<>();
		LocalDate start = from;
		while (!start.isAfter(to)) {
			LocalDate end = switch (unit) {
				case DAY -> start;
				case WEEK -> start.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
				case MONTH -> start.with(TemporalAdjusters.lastDayOfMonth());
			};
			if (end.isAfter(to)) {
				end = to;
			}
			periods.add(new Period(start, end));
			start = end.plusDays(1);
		}
		return periods;
	}

	/** 조회 기간 하나. start·end 둘 다 포함. (계산 메서드라 테스트에서 바로 부를 수 있게 public) */
	public record Period(LocalDate start, LocalDate end) {

		public long days() {
			return ChronoUnit.DAYS.between(start, end) + 1;
		}
	}

	private void validatePeriod(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new BusinessException("조회 시작일은 종료일보다 늦을 수 없습니다.");
		}
		if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
			throw new BusinessException("조회 기간은 1년 이내로 선택해 주세요.");
		}
	}

	private static BigDecimal percent(long part, long whole) {
		if (whole == 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		return BigDecimal.valueOf(part).multiply(HUNDRED).divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
	}

	private static BigDecimal money(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	/** CSV 한 줄. 쉼표·따옴표·줄바꿈이 들어간 값은 따옴표로 감쌉니다(채널 이름 등). 줄 끝은 엑셀 기준 \r\n. */
	private static void line(StringBuilder csv, String... values) {
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				csv.append(',');
			}
			String value = values[i];
			if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
				value = "\"" + value.replace("\"", "\"\"") + "\"";
			}
			csv.append(value);
		}
		csv.append("\r\n");
	}
}

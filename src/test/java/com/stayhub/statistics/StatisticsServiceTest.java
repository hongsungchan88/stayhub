package com.stayhub.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stayhub.common.BusinessException;
import com.stayhub.payment.entity.Payment;
import com.stayhub.payment.entity.PaymentRefundRule;
import com.stayhub.payment.repository.PaymentRepository;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.entity.ReservationStatus;
import com.stayhub.reservation.repository.ReservationRepository;
import com.stayhub.reservation.service.ReservationService;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import com.stayhub.statistics.dto.ChannelStatisticsResponse;
import com.stayhub.statistics.dto.ExportType;
import com.stayhub.statistics.dto.OccupancyResponse;
import com.stayhub.statistics.dto.RevenueResponse;
import com.stayhub.statistics.dto.StatisticsUnit;
import com.stayhub.statistics.service.StatisticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통계 서비스 테스트 (STA-001~004).
 *
 * <p><b>이 파일이 하는 일</b>
 * <br>객실·예약·결제를 직접 넣고, 통계가 총괄 결정대로 계산되는지 확인합니다.
 * {@code @Transactional} 이라 테스트가 끝나면 넣고 지운 데이터가 전부 되돌려집니다(롤백).
 *
 * <p><b>날짜 주의</b>
 * <br>예약·결제의 {@code created_at} 은 저장할 때 자동으로 "지금"이 들어가서 지난 날짜로 만들 수 없습니다.
 * 그래서 예약 건수·매출은 오늘 기준으로 확인하고, 증감률 계산식은 {@link StatisticsService#growthRate} 로 따로 확인합니다.
 * 투숙 날짜(check_in·check_out)는 자유롭게 정할 수 있어서 가동률은 30일 뒤 날짜로 확인합니다.
 *
 * <p>예약은 반드시 {@link ReservationService#create} 로 만듭니다(겹침 검사·동시 예약 차단이 서비스에만 있음).
 * 채널 1번 "직접 예약"·2번 "기타 채널" 은 data.sql 로 들어가 있습니다.
 */
@SpringBootTest
@Transactional
class StatisticsServiceTest {

	private static final LocalDate TODAY = LocalDate.now();

	@Autowired
	private StatisticsService statisticsService;

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	private Room room1;
	private Room room2;

	@BeforeEach
	void setUp() {
		// 개발 DB 에 이미 들어 있는 데이터 때문에 결과가 흔들리지 않도록 테스트 안에서만 비웁니다.
		// 경고: @Transactional을 지우면 개발 DB의 이 테이블들이 실제로 비워집니다. 지우지 마세요.
		paymentRepository.deleteAll();
		reservationRepository.deleteAll();
		roomRepository.deleteAll();

		room1 = roomRepository.save(Room.create(1L, "T101", "더블", new BigDecimal("100000.00"), 2));
		room2 = roomRepository.save(Room.create(1L, "T102", "더블", new BigDecimal("100000.00"), 2));
	}

	@Test
	@DisplayName("가동률: 투숙 중인 CONFIRMED·CHECKED_IN 만 세고, CANCELLED·NOSHOW 는 빼고, 날짜별로 객실 수 ÷ 전체 객실 수")
	void occupancyCountsOnlySoldStatuses() {
		LocalDate base = TODAY.plusDays(30);
		reserve(room1, null, base, base.plusDays(2));                                   // base, base+1 투숙 (CONFIRMED)
		Reservation checkedIn = reserve(room2, null, base, base.plusDays(1));           // base 투숙
		reservationService.changeStatus(checkedIn.getId(), ReservationStatus.CHECKED_IN);
		Reservation cancelled = reserve(room2, null, base.plusDays(1), base.plusDays(2)); // 취소 → 안 셈
		reservationService.changeStatus(cancelled.getId(), ReservationStatus.CANCELLED);
		Reservation noShow = reserve(room2, null, base.plusDays(2), base.plusDays(3));     // 노쇼 → 안 셈
		reservationService.changeStatus(noShow.getId(), ReservationStatus.NOSHOW);

		OccupancyResponse response = statisticsService.getOccupancy(base, base.plusDays(2), StatisticsUnit.DAY);

		assertThat(response.totalRooms()).isEqualTo(2);
		assertThat(response.hasData()).isTrue();
		assertThat(response.items()).extracting(OccupancyResponse.Item::occupancyRate)
				.containsExactly(new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("0.00"));
		// 체크아웃 날은 투숙이 아니라서 base+2 의 room1 은 세지 않는다 (check_in ≤ 그날 < check_out)
		assertThat(response.items().get(2).soldRoomNights()).isZero();
	}

	@Test
	@DisplayName("가동률: 예약이 없는 기간이면 hasData=false, 가동률 0.00")
	void occupancyWithoutDataIsEmpty() {
		LocalDate base = TODAY.plusDays(100);

		OccupancyResponse response = statisticsService.getOccupancy(base, base.plusDays(1), StatisticsUnit.DAY);

		assertThat(response.hasData()).isFalse();
		assertThat(response.items()).extracting(OccupancyResponse.Item::occupancyRate)
				.containsOnly(new BigDecimal("0.00"));
	}

	@Test
	@DisplayName("채널별: 채널 없는 예약은 1번 직접 예약으로, CANCELLED 는 빼고, 매출은 결제 채널별")
	void channelsGroupByChannel() {
		LocalDate base = TODAY.plusDays(30);
		Reservation direct = reserve(room1, null, base, base.plusDays(1));      // channel_id NULL → 1번
		Reservation cancelled = reserve(room1, 1L, base.plusDays(1), base.plusDays(2));
		reservationService.changeStatus(cancelled.getId(), ReservationStatus.CANCELLED); // 안 셈
		Reservation other = reserve(room2, 2L, base, base.plusDays(1));          // 2번 기타 채널

		paymentRepository.save(Payment.create(direct.getId(), 1L, "PREPAID",
				new BigDecimal("100000.00"), BigDecimal.ZERO, new BigDecimal("100000.00"), null));
		paymentRepository.save(Payment.create(other.getId(), 2L, "PREPAID",
				new BigDecimal("50000.00"), new BigDecimal("5000.00"), new BigDecimal("45000.00"), null));

		ChannelStatisticsResponse response = statisticsService.getChannels(TODAY, TODAY, null);

		ChannelStatisticsResponse.Item channel1 = itemOf(response, 1L);
		assertThat(channel1.channelName()).isEqualTo("직접 예약");
		assertThat(channel1.bookingCount()).isEqualTo(1);
		assertThat(channel1.salesAmount()).isEqualByComparingTo("100000.00");

		ChannelStatisticsResponse.Item channel2 = itemOf(response, 2L);
		assertThat(channel2.bookingCount()).isEqualTo(1);
		assertThat(channel2.salesAmount()).isEqualByComparingTo("45000.00");

		// channelId 를 지정하면 그 채널만
		assertThat(statisticsService.getChannels(TODAY, TODAY, 2L).items())
				.extracting(ChannelStatisticsResponse.Item::channelId).containsExactly(2L);
	}

	@Test
	@DisplayName("매출: 부분 환불은 비율만큼 빼고, 전액 환불은 0, 전월 매출이 0 이면 증감률 null")
	void revenueAppliesRefundRules() {
		LocalDate base = TODAY.plusDays(30);
		Reservation r1 = reserve(room1, null, base, base.plusDays(1));
		Reservation r2 = reserve(room2, 2L, base, base.plusDays(1));
		Reservation r3 = reserve(room1, null, base.plusDays(1), base.plusDays(2));

		// 그대로 결제 완료 → 100,000
		paymentRepository.save(Payment.create(r1.getId(), 1L, "PREPAID",
				new BigDecimal("100000.00"), BigDecimal.ZERO, new BigDecimal("100000.00"), null));
		// 50,000 결제(실매출 45,000) 중 10,000 부분 환불 → 45,000 × (1 − 10,000 ÷ 50,000) = 36,000
		Payment partial = Payment.create(r2.getId(), 2L, "PREPAID",
				new BigDecimal("50000.00"), new BigDecimal("5000.00"), new BigDecimal("45000.00"), null);
		partial.requestRefund(TODAY);
		partial.refund(PaymentRefundRule.PARTIAL, new BigDecimal("10000.00"));
		paymentRepository.save(partial);
		// 전액 환불 → 0
		Payment full = Payment.create(r3.getId(), 1L, "PREPAID",
				new BigDecimal("80000.00"), BigDecimal.ZERO, new BigDecimal("80000.00"), null);
		full.requestRefund(TODAY);
		full.refund(PaymentRefundRule.FULL, new BigDecimal("80000.00"));
		paymentRepository.save(full);

		RevenueResponse response = statisticsService.getRevenue(TODAY, TODAY, StatisticsUnit.DAY);

		assertThat(response.totalRevenue()).isEqualByComparingTo("136000.00");
		assertThat(response.items()).hasSize(1);
		assertThat(response.previousStartDate()).isEqualTo(TODAY.minusMonths(1));
		assertThat(response.previousRevenue()).isEqualByComparingTo("0");
		assertThat(response.growthRate()).isNull(); // 비교 불가
	}

	@Test
	@DisplayName("증감률 계산: 소수 둘째 자리 반올림, 비교 매출 0 이면 null")
	void growthRateCalculation() {
		assertThat(StatisticsService.growthRate(new BigDecimal("150"), new BigDecimal("100")))
				.isEqualByComparingTo("50.00");
		assertThat(StatisticsService.growthRate(new BigDecimal("100"), new BigDecimal("300")))
				.isEqualByComparingTo("-66.67");
		assertThat(StatisticsService.growthRate(new BigDecimal("100"), BigDecimal.ZERO)).isNull();
	}

	@Test
	@DisplayName("비교 기간 종료일: 월말이면 지난달 월말로 (11/30 -> 10/31), 아니면 한 달 전 같은 날")
	void previousEndDateKeepsMonthEnd() {
		assertThat(StatisticsService.previousEndDate(LocalDate.of(2026, 11, 30))).isEqualTo(LocalDate.of(2026, 10, 31));
		assertThat(StatisticsService.previousEndDate(LocalDate.of(2026, 3, 31))).isEqualTo(LocalDate.of(2026, 2, 28));
		assertThat(StatisticsService.previousEndDate(LocalDate.of(2026, 11, 15))).isEqualTo(LocalDate.of(2026, 10, 15));
	}

	@Test
	@DisplayName("구간 나누기: 주는 월요일 시작, 월은 달력 월, 첫·마지막 구간은 조회 기간에 맞춰 잘림")
	void periodsSplitByUnit() {
		LocalDate from = LocalDate.of(2026, 11, 27); // 금요일
		LocalDate to = LocalDate.of(2026, 12, 2);

		assertThat(StatisticsService.periods(from, to, StatisticsUnit.WEEK)).containsExactly(
				new StatisticsService.Period(LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 29)),
				new StatisticsService.Period(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 2)));
		assertThat(StatisticsService.periods(from, to, StatisticsUnit.MONTH)).containsExactly(
				new StatisticsService.Period(LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 30)),
				new StatisticsService.Period(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2)));
		assertThat(StatisticsService.periods(from, to, StatisticsUnit.DAY)).hasSize(6);
	}

	@Test
	@DisplayName("CSV: 맨 앞에 UTF-8 BOM, 머리글 포함, 비교 불가 표시")
	void exportCsvHasBom() {
		String occupancy = statisticsService.exportCsv(ExportType.OCCUPANCY, TODAY, TODAY, StatisticsUnit.DAY);
		assertThat(occupancy).startsWith("\uFEFF시작일,종료일,판매 객실·일,판매 가능 객실·일,가동률(%)\r\n");

		String revenue = statisticsService.exportCsv(ExportType.REVENUE, TODAY, TODAY, StatisticsUnit.DAY);
		assertThat(revenue).contains("전월 대비 증감률(%),,비교 불가");

		String channel = statisticsService.exportCsv(ExportType.CHANNEL, TODAY, TODAY, StatisticsUnit.DAY);
		assertThat(channel).contains("직접 예약,");
	}

	@Test
	@DisplayName("조회 기간이 거꾸로이거나 1년을 넘으면 BusinessException")
	void invalidPeriodThrows() {
		assertThatThrownBy(() -> statisticsService.getOccupancy(TODAY, TODAY.minusDays(1), StatisticsUnit.DAY))
				.isInstanceOf(BusinessException.class)
				.hasMessage("조회 시작일은 종료일보다 늦을 수 없습니다.");
		assertThatThrownBy(() -> statisticsService.getRevenue(TODAY, TODAY.plusDays(366), StatisticsUnit.DAY))
				.isInstanceOf(BusinessException.class)
				.hasMessage("조회 기간은 1년 이내로 선택해 주세요.");
	}

	/** 예약은 반드시 ReservationService.create 로. 게스트 정보는 고정값입니다. */
	private Reservation reserve(Room room, Long channelId, LocalDate checkIn, LocalDate checkOut) {
		return reservationService.create(null, "통계테스트", "010-0000-0000", room.getId(), channelId, checkIn, checkOut, 1);
	}

	private ChannelStatisticsResponse.Item itemOf(ChannelStatisticsResponse response, Long channelId) {
		return response.items().stream()
				.filter(item -> item.channelId().equals(channelId))
				.findFirst()
				.orElseThrow();
	}
}

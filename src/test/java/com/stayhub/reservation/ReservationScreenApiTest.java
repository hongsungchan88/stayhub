package com.stayhub.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import com.stayhub.common.PropertyScope;
import com.stayhub.reservation.dto.ReservationCreateRequest;
import com.stayhub.reservation.dto.ReservationResponse;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.service.ReservationService;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import com.stayhub.common.BusinessException;
import com.stayhub.reservation.dto.ReservationUpdateRequest;

/**
 * 예약 등록 API·4가지 검색·페이지 분할을 확인합니다.
 * 테스트 데이터는 종료 시 롤백합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationScreenApiTest {

    private static final LocalDate DAY = LocalDate.of(2070, 1, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long roomId;
    private String tag;

    @BeforeEach
    void setUp() {
        tag = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 12);

        roomId = roomRepository.save(Room.create(
                PropertyScope.DEFAULT_PROPERTY_ID,
                "R" + tag,
                "STANDARD",
                new BigDecimal("100000.00"),
                2
        )).getId();
    }

    @Test
    @DisplayName("예약 등록 API는 성공 응답을 반환하고 같은 객실·날짜의 중복 등록은 거절한다")
    void createAndRejectDuplicate() throws Exception {
        ReservationCreateRequest request = new ReservationCreateRequest(
                "Guest" + tag, "010-1111-1111", roomId, null,
                DAY, DAY.plusDays(1), 1
        );

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                LoginUser.SESSION_KEY,
                new LoginUser(1L, "tester", "테스트사용자", UserRole.HOST)
        );

        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/reservations")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.channelName").value("직접 예약"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/reservations")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("선택한 날짜에 이미 예약된 객실입니다."));
    }

    @Test
    @DisplayName("이름·연락처·예약번호·채널 검색이 해당 예약만 반환한다")
    void fourSearchTypes() {
        String channelName = "OTA" + tag;

        jdbcTemplate.update("""
                INSERT INTO channels (
                    channel_name, commission_rate, default_commission_rate,
                    is_registered, created_at, updated_at
                ) VALUES (?, 10.00, 10.00, TRUE, NOW(), NOW())
                """, channelName);

        Long channelId = jdbcTemplate.queryForObject(
                "SELECT channel_id FROM channels WHERE channel_name = ?",
                Long.class,
                channelName
        );

        Reservation target = reservationService.create(
                null, "Guest" + tag, "555" + tag, roomId, channelId,
                DAY, DAY.plusDays(1), 1
        );

        reservationService.create(
                null, "Other" + tag, "444" + tag, roomId, null,
                DAY.plusDays(1), DAY.plusDays(2), 1
        );

        assertSearch("guestName", "Guest" + tag, target.getId());
        assertSearch("guestPhone", "555" + tag, target.getId());
        assertSearch("reservationCode", target.getReservationCode(), target.getId());
        assertSearch("channelName", channelName, target.getId());
    }

    @Test
    @DisplayName("검색 결과 21건은 첫 페이지 20건과 다음 페이지 1건으로 나뉜다")
    void pagination() {
        for (int i = 0; i < 21; i++) {
            reservationService.create(
                    null, "Page" + tag, "010-1111-1111", roomId, null,
                    DAY.plusDays(i), DAY.plusDays(i + 1), 1
            );
        }

        var first = reservationService.getList("guestName", "Page" + tag, 0);
        var second = reservationService.getList("guestName", "Page" + tag, 1);

        assertThat(first.content()).hasSize(20);
        assertThat(second.content()).hasSize(1);
        assertThat(first.totalElements()).isEqualTo(21);
        assertThat(first.totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("예약 상세 API와 상세 화면이 해당 예약을 보여준다")
    void detailLookup() throws Exception {
        Reservation reservation = reservationService.create(
                null, "Detail" + tag, "010-1111-1111", roomId, null,
                DAY, DAY.plusDays(1), 1
        );

        mockMvc.perform(get("/api/reservations/{id}", reservation.getId())
                        .session(hostSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(reservation.getId()))
                .andExpect(jsonPath("$.data.guestName").value("Detail" + tag))
                .andExpect(jsonPath("$.data.roomNumber").value("R" + tag))
                .andExpect(jsonPath("$.data.channelName").value("직접 예약"))
                .andExpect(jsonPath("$.data.checkInDate").value("2070-01-01"));

        mockMvc.perform(get("/admin/reservations/{id}", reservation.getId())
                        .session(hostSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservations/detail"));
    }

    @Test
    @DisplayName("없는 예약을 상세 조회하면 안내 메시지를 반환한다")
    void missingReservation() throws Exception {
        mockMvc.perform(get("/api/reservations/{id}", Long.MAX_VALUE)
                        .session(hostSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("예약을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("자기 예약은 중복으로 보지 않고 날짜를 변경할 수 있다")
    void updateOwnReservation() throws Exception {
        Reservation reservation = reservationService.create(
                null, "Update" + tag, "010-1111-1111", roomId, null,
                DAY, DAY.plusDays(1), 1
        );

        // 날짜와 객실을 그대로 저장해도 자기 자신과 중복됐다고 거절하면 안 됩니다.
        ReservationUpdateRequest unchanged = new ReservationUpdateRequest(
                roomId, DAY, DAY.plusDays(1)
        );

        mockMvc.perform(put("/api/reservations/{id}", reservation.getId())
                        .session(hostSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unchanged)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(reservation.getId()));

        // 날짜를 바꿔도 기존 예약번호와 예약자 정보는 유지됩니다.
        String originalCode = reservation.getReservationCode();

        ReservationResponse changed = reservationService.update(
                reservation.getId(),
                new ReservationUpdateRequest(
                        roomId, DAY.plusDays(2), DAY.plusDays(3)
                )
        );

        assertThat(changed.id()).isEqualTo(reservation.getId());
        assertThat(changed.reservationCode()).isEqualTo(originalCode);
        assertThat(changed.guestName()).isEqualTo("Update" + tag);
        assertThat(changed.checkInDate()).isEqualTo(DAY.plusDays(2));
        assertThat(changed.checkOutDate()).isEqualTo(DAY.plusDays(3));
    }

    @Test
    @DisplayName("다른 예약과 겹치는 날짜로 변경하면 거절하고 기존 날짜를 유지한다")
    void overlappingUpdateRejected() {
        Reservation target = reservationService.create(
                null, "Target" + tag, "010-1111-1111", roomId, null,
                DAY, DAY.plusDays(1), 1
        );

        reservationService.create(
                null, "Other" + tag, "010-2222-2222", roomId, null,
                DAY.plusDays(2), DAY.plusDays(3), 1
        );

        assertThatThrownBy(() -> reservationService.update(
                target.getId(),
                new ReservationUpdateRequest(
                        roomId, DAY.plusDays(2), DAY.plusDays(3)
                )
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("선택한 날짜에 이미 예약된 객실입니다.");

        assertThat(target.getCheckInDate()).isEqualTo(DAY);
        assertThat(target.getCheckOutDate()).isEqualTo(DAY.plusDays(1));
    }

    @Test
    @DisplayName("예약 인원을 수용할 수 없는 객실로 변경하면 거절한다")
    void smallerRoomRejected() {
        Reservation target = reservationService.create(
                null, "Capacity" + tag, "010-1111-1111", roomId, null,
                DAY, DAY.plusDays(1), 2
        );

        Long smallerRoomId = roomRepository.save(Room.create(
                PropertyScope.DEFAULT_PROPERTY_ID,
                "S" + tag,
                "STANDARD",
                new BigDecimal("100000.00"),
                1
        )).getId();

        assertThatThrownBy(() -> reservationService.update(
                target.getId(),
                new ReservationUpdateRequest(
                        smallerRoomId, DAY, DAY.plusDays(1)
                )
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("roomId:")
                .hasMessageContaining("초과합니다.");

        assertThat(target.getRoomId()).isEqualTo(roomId);
    }

    @Test
    @DisplayName("취소된 예약의 날짜와 객실 변경을 거절한다")
    void cancelledReservationUpdateRejected() {
        Reservation target = reservationService.create(
                null, "Cancelled" + tag, "010-1111-1111", roomId, null,
                DAY, DAY.plusDays(1), 1
        );

        reservationService.changeStatus(
                target.getId(),
                com.stayhub.reservation.entity.ReservationStatus.CANCELLED
        );

        assertThatThrownBy(() -> reservationService.update(
                target.getId(),
                new ReservationUpdateRequest(
                        roomId, DAY.plusDays(2), DAY.plusDays(3)
                )
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "예약 확정 상태에서만 날짜와 객실을 변경할 수 있습니다.");
    }

    private MockHttpSession hostSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                LoginUser.SESSION_KEY,
                new LoginUser(1L, "tester", "테스트사용자", UserRole.HOST)
        );
        return session;
    }

    private void assertSearch(String type, String keyword, Long id) {
        var result = reservationService.getList(type, keyword, 0);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content())
                .extracting(ReservationResponse::id)
                .containsExactly(id);
    }
}
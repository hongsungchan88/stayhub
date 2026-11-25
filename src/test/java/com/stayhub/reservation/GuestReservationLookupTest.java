package com.stayhub.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.stayhub.common.PropertyScope;
import com.stayhub.reservation.entity.Reservation;
import com.stayhub.reservation.repository.ReservationRepository;
import com.stayhub.room.entity.Room;
import com.stayhub.room.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** GST-04의 예약 본인 확인과 게스트 세션 접근 범위를 확인합니다. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GuestReservationLookupTest {

    private static final String LOOKUP_URL = "/api/public/reservations/lookup";
    private static final String PHONE = "010-1234-5678";
    private static final String MISMATCH = "예약번호와 연락처를 다시 확인해 주세요.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservations;

    @Autowired
    private RoomRepository rooms;

    private Reservation reservation;

    @BeforeEach
    void setUp() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Room room = rooms.save(Room.create(PropertyScope.DEFAULT_PROPERTY_ID,
                "G-" + tag, "STANDARD", new BigDecimal("89000"), 2));
        LocalDate arrival = LocalDate.now().plusDays(20);
        reservation = reservations.saveAndFlush(Reservation.create("GST-" + tag, null,
                "테스트 손님", PHONE, room.getId(), null, arrival, arrival.plusDays(2), 2));
    }

    @Test
    @DisplayName("비로그인 상태로 조회 화면과 메뉴를 열 수 있다")
    void opensGuestPage() throws Exception {
        mockMvc.perform(get("/my-reservation"))
                .andExpect(status().isOk())
                .andExpect(view().name("guest/my-reservation"))
                .andExpect(content().string(Matchers.containsString("id=\"lookup-form\"")));

        mockMvc.perform(get("/"))
                .andExpect(content().string(Matchers.containsString("href=\"/my-reservation\"")));
    }

    @Test
    @DisplayName("번호와 연락처 모두 일치하면 같은 세션에서만 예약 한 건을 조회할 수 있다")
    void verifiesOnlyOneReservationWithoutAdminAccess() throws Exception {
        MockHttpSession session = (MockHttpSession) verify(reservation.getReservationCode(), PHONE, null)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.data.reservationCode").value(reservation.getReservationCode()))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.guestCount").value(2))
                .andExpect(jsonPath("$.data.guestPhone").doesNotExist())
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andReturn().getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get(LOOKUP_URL).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reservationCode").value(reservation.getReservationCode()));
        mockMvc.perform(get(LOOKUP_URL).session(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
        mockMvc.perform(get("/api/reservations").session(session))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin").session(session))
                .andExpect(redirectedUrl("/login?expired"));
    }

    @Test
    @DisplayName("연락처가 다르거나 예약번호가 없으면 같은 안내를 주고 상세 조회를 막는다")
    void rejectsWrongPairAndClearsPreviousLookup() throws Exception {
        MockHttpSession session = (MockHttpSession) verify(reservation.getReservationCode(), PHONE, null)
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
        assertThat(session).isNotNull();

        verify(reservation.getReservationCode(), "010-0000-0000", session)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(MISMATCH));
        verify("NO-SUCH-RESERVATION", PHONE, session)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(MISMATCH));
        mockMvc.perform(get(LOOKUP_URL).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }

    private ResultActions verify(String code, String phone, MockHttpSession session) throws Exception {
        MockHttpServletRequestBuilder request = post(LOOKUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reservationCode":"%s","guestPhone":"%s"}
                        """.formatted(code, phone));
        if (session != null) {
            request.session(session);
        }
        return mockMvc.perform(request);
    }
}

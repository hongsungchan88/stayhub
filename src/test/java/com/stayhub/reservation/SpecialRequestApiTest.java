package com.stayhub.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.User;
import com.stayhub.account.entity.UserRole;
import com.stayhub.account.repository.UserRepository;
import com.stayhub.common.BusinessException;
import com.stayhub.common.PropertyScope;
import com.stayhub.reservation.dto.SpecialRequestCreateRequest;
import com.stayhub.reservation.dto.SpecialRequestUpdateRequest;
import com.stayhub.reservation.entity.SpecialRequestStatus;
import com.stayhub.reservation.service.ReservationService;
import com.stayhub.reservation.service.SpecialRequestService;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SpecialRequestApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpecialRequestService specialRequestService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long reservationId;
    private Long otherReservationId;
    private String tag;

    @BeforeEach
    void setUp() {
        tag = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 12);

        Long roomId = roomRepository.save(Room.create(
                PropertyScope.DEFAULT_PROPERTY_ID,
                "S" + tag,
                "STANDARD",
                new BigDecimal("100000.00"),
                2
        )).getId();

        LocalDate day = LocalDate.of(2070, 1, 1);

        reservationId = reservationService.create(
                null, "Guest" + tag, "010-1111-1111",
                roomId, null, day, day.plusDays(1), 1
        ).getId();

        otherReservationId = reservationService.create(
                null, "Other" + tag, "010-2222-2222",
                roomId, null, day.plusDays(1), day.plusDays(2), 1
        ).getId();
    }

    @Test
    @DisplayName("요청 등록 API는 접수 상태와 날짜를 반환하고 화면을 열 수 있다")
    void createAndOpenPage() throws Exception {
        var request = new SpecialRequestCreateRequest(
                reservationId, "베개를 추가해 주세요."
        );

        mockMvc.perform(post("/api/special-requests")
                        .session(hostSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.reservationId").value(reservationId))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

        mockMvc.perform(get("/admin/special-requests")
                        .session(hostSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/special-requests/list"));
    }

    @Test
    @DisplayName("빈 요청 내용과 존재하지 않는 예약의 등록을 거절한다")
    void invalidCreateRejected() throws Exception {
        var blank = new SpecialRequestCreateRequest(
                reservationId, "   "
        );

        mockMvc.perform(post("/api/special-requests")
                        .session(hostSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blank)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertThatThrownBy(() -> specialRequestService.create(
                new SpecialRequestCreateRequest(
                        Long.MAX_VALUE, "베개 추가"
                )
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("reservationId: 예약을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("예약별 조회는 다른 예약을 제외하고 21건을 20건과 1건으로 나눈다")
    void filterAndPagination() {
        for (int i = 0; i < 21; i++) {
            specialRequestService.create(
                    new SpecialRequestCreateRequest(
                            reservationId, "요청 " + i
                    )
            );
        }

        specialRequestService.create(
                new SpecialRequestCreateRequest(
                        otherReservationId, "다른 예약의 요청"
                )
        );

        var first = specialRequestService.getList(reservationId, 0);
        var second = specialRequestService.getList(reservationId, 1);

        assertThat(first.content()).hasSize(20);
        assertThat(second.content()).hasSize(1);
        assertThat(first.totalElements()).isEqualTo(21);
        assertThat(first.totalPages()).isEqualTo(2);

        assertThat(first.content()).allSatisfy(item ->
                assertThat(item.reservationId()).isEqualTo(reservationId));
    }

    @Test
    @DisplayName("거절 사유는 필수이며 다른 상태로 변경하면 사유를 지운다")
    void rejectionReasonValidation() {
        Long id = specialRequestService.create(
                new SpecialRequestCreateRequest(
                        reservationId, "늦은 체크아웃 요청"
                )
        ).id();

        assertThatThrownBy(() -> specialRequestService.update(
                id,
                new SpecialRequestUpdateRequest(
                        SpecialRequestStatus.REJECTED, "  ", null
                )
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("rejectReason: 거절 사유를 입력해 주세요.");

        var rejected = specialRequestService.update(
                id,
                new SpecialRequestUpdateRequest(
                        SpecialRequestStatus.REJECTED,
                        "지원하지 않는 요청입니다.",
                        null
                )
        );

        assertThat(rejected.status()).isEqualTo(SpecialRequestStatus.REJECTED);
        assertThat(rejected.rejectReason())
                .isEqualTo("지원하지 않는 요청입니다.");

        var received = specialRequestService.update(
                id,
                new SpecialRequestUpdateRequest(
                        SpecialRequestStatus.RECEIVED, null, null
                )
        );

        assertThat(received.status()).isEqualTo(SpecialRequestStatus.RECEIVED);
        assertThat(received.rejectReason()).isNull();
    }

    @Test
    @DisplayName("담당자를 배정하고 해제할 수 있으며 없는 담당자는 거절한다")
    void assignAndClearStaff() {
        Long staffId = userRepository.saveAndFlush(User.create(
                "staff" + tag,
                passwordEncoder.encode("Test1234!"),
                "테스트 담당자",
                "010-3333-3333",
                tag + "@example.com",
                UserRole.FRONT
        )).getId();

        Long id = specialRequestService.create(
                new SpecialRequestCreateRequest(
                        reservationId, "침구 추가"
                )
        ).id();

        var assigned = specialRequestService.update(
                id,
                new SpecialRequestUpdateRequest(
                        SpecialRequestStatus.IN_PROGRESS, null, staffId
                )
        );

        assertThat(assigned.status())
                .isEqualTo(SpecialRequestStatus.IN_PROGRESS);
        assertThat(assigned.assignedStaffId()).isEqualTo(staffId);

        var completed = specialRequestService.update(
                id,
                new SpecialRequestUpdateRequest(
                        SpecialRequestStatus.COMPLETED, null, null
                )
        );

        assertThat(completed.status())
                .isEqualTo(SpecialRequestStatus.COMPLETED);
        assertThat(completed.assignedStaffId()).isNull();

        assertThatThrownBy(() -> specialRequestService.update(
                id,
                new SpecialRequestUpdateRequest(
                        SpecialRequestStatus.IN_PROGRESS,
                        null,
                        Long.MAX_VALUE
                )
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "assignedStaffId: 사용 중인 담당자 계정을 찾을 수 없습니다.");
    }

    private MockHttpSession hostSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                LoginUser.SESSION_KEY,
                new LoginUser(1L, "tester", "테스트사용자", UserRole.HOST)
        );
        return session;
    }
}
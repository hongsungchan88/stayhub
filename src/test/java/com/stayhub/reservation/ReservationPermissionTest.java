package com.stayhub.reservation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("로그인하지 않으면 취소·노쇼·대상 조회를 401로 거절한다")
    void loginRequired() throws Exception {
        mockMvc.perform(get("/api/reservations/no-show-candidates"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/reservations/{id}/cancel", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancelReason\":null}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/reservations/{id}/no-show", Long.MAX_VALUE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("청소·일반 스태프는 취소·노쇼·대상 조회를 403으로 거절한다")
    void otherRolesForbidden() throws Exception {
        for (UserRole role : new UserRole[]{
                UserRole.CLEANING, UserRole.STAFF
        }) {
            mockMvc.perform(get("/api/reservations/no-show-candidates")
                            .session(session(role)))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post(
                            "/api/reservations/{id}/cancel", Long.MAX_VALUE)
                            .session(session(role))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"cancelReason\":null}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post(
                            "/api/reservations/{id}/no-show", Long.MAX_VALUE)
                            .session(session(role)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("호스트·프런트는 권한 검사를 통과한다")
    void hostAndFrontAllowed() throws Exception {
        for (UserRole role : new UserRole[]{
                UserRole.HOST, UserRole.FRONT
        }) {
            mockMvc.perform(get("/api/reservations/no-show-candidates")
                            .session(session(role)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // 없는 예약 ID를 사용합니다.
            // 권한 검사를 통과한 뒤 예약 조회 단계에 도달하는지 확인합니다.
            mockMvc.perform(post(
                            "/api/reservations/{id}/cancel", Long.MAX_VALUE)
                            .session(session(role))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"cancelReason\":null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("예약을 찾을 수 없습니다."));

            mockMvc.perform(post(
                            "/api/reservations/{id}/no-show", Long.MAX_VALUE)
                            .session(session(role)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("예약을 찾을 수 없습니다."));
        }
    }

    private MockHttpSession session(UserRole role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                LoginUser.SESSION_KEY,
                new LoginUser(1L, "tester", "권한 테스트", role)
        );
        return session;
    }
}
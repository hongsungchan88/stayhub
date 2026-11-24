package com.stayhub.reservation;

import com.stayhub.account.dto.LoginUser;
import com.stayhub.account.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckInOutPermissionTest {
    @Autowired MockMvc mvc;
    @Test void sessionAndRoleRequired() throws Exception {
        String base = "/api/reservations";
        mvc.perform(get(base + "/check-in-out")).andExpect(status().isUnauthorized());
        for (var role : new UserRole[]{UserRole.CLEANING, UserRole.STAFF}) {
            var u = new LoginUser(1L, "tester", "테스트", role);
            mvc.perform(get("/admin/check-in-out").sessionAttr(LoginUser.SESSION_KEY, u))
                    .andExpect(status().isForbidden());
            mvc.perform(get(base + "/check-in-out").sessionAttr(LoginUser.SESSION_KEY, u))
                    .andExpect(status().isForbidden());
            mvc.perform(post(base + "/1/check-in").sessionAttr(LoginUser.SESSION_KEY, u)
                            .contentType("application/json").content("{\"keyCode\":\"K-1\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post(base + "/1/check-out").sessionAttr(LoginUser.SESSION_KEY, u))
                    .andExpect(status().isForbidden());
        }
        for (var role : new UserRole[]{UserRole.HOST, UserRole.FRONT}) {
            mvc.perform(get(base + "/check-in-out").sessionAttr(LoginUser.SESSION_KEY,
                    new LoginUser(1L, "tester", "테스트", role))).andExpect(status().isOk());
        }
    }
}
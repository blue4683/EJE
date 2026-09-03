package com.skala.miniproject.user;

import com.skala.miniproject.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void restoreSeedUserPlans() {
        jdbcTemplate.update("UPDATE users SET plan = 'FREE' WHERE id = 1");
        jdbcTemplate.update("UPDATE users SET plan = 'PRO' WHERE id = 2");
    }

    @Test
    void 내_정보는_인증된_사용자의_명세_User_DTO를_반환한다() throws Exception {
        mockMvc.perform(get("/users/me")
                        .servletPath("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.name").value("사용자"))
                .andExpect(jsonPath("$.data.profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.plan").value("FREE"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-08-27T09:00:00Z"));
    }

    @Test
    void 같은_Access_토큰이어도_변경된_DB_plan을_매_요청_반영한다() throws Exception {
        String accessToken = bearerToken(1L);

        mockMvc.perform(get("/users/me")
                        .servletPath("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan").value("FREE"));

        jdbcTemplate.update("UPDATE users SET plan = 'PRO' WHERE id = 1");

        mockMvc.perform(get("/users/me")
                        .servletPath("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan").value("PRO"));
    }

    @Test
    void PRO_사용자의_내_정보는_DB의_PRO_plan을_반환한다() throws Exception {
        mockMvc.perform(get("/users/me")
                        .servletPath("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("2"))
                .andExpect(jsonPath("$.data.plan").value("PRO"));
    }

    @Test
    void Access_토큰이_없으면_401_envelope을_반환한다() throws Exception {
        mockMvc.perform(get("/users/me").servletPath("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("로그인이 필요합니다."));
    }

    private String bearerToken(Long userId) {
        return "Bearer " + tokenProvider.issueAccessToken(userId);
    }
}

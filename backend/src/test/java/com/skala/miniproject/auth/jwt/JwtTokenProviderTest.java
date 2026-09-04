package com.skala.miniproject.auth.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.common.web.NoStoreCacheFilter;
import com.skala.miniproject.config.JwtProperties;
import com.skala.miniproject.config.SecurityConfig;
import com.skala.miniproject.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JwtTokenProviderTest {

    private static final String ACCESS_SECRET = "access-secret-that-is-at-least-32-bytes-long";
    private static final String REFRESH_SECRET = "refresh-secret-that-is-at-least-32-bytes-long";

    private final JwtProperties properties = new JwtProperties(
            "speech-service",
            "speech-api",
            "speech-auth",
            1_800,
            1_209_600,
            ACCESS_SECRET,
            REFRESH_SECRET
    );
    private final JwtTokenProvider tokenProvider = new JwtTokenProvider(properties);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_Access에서_사용자_ID를_꺼낸다() {
        String token = tokenProvider.issueAccessToken(42L);

        assertThat(tokenProvider.validateAccessToken(token)).isEqualTo(42L);
    }

    @Test
    void 유효한_Refresh에서_사용자_ID를_꺼낸다() {
        String token = tokenProvider.issueRefreshToken(42L);

        assertThat(tokenProvider.validateRefreshToken(token)).isEqualTo(42L);
    }

    @Test
    void 만료된_Access는_거절한다() throws Exception {
        Instant now = Instant.now();
        String token = signedToken(
                ACCESS_SECRET,
                "speech-api",
                TokenType.ACCESS,
                now.minusSeconds(1_801),
                now.minusSeconds(1)
        );

        assertAuthenticationFailure(tokenProvider::validateAccessToken, token, ErrorCode.UNAUTHORIZED);
    }

    @Test
    void alg를_none으로_바꾼_토큰은_거절한다() {
        String validToken = tokenProvider.issueAccessToken(42L);
        String payload = validToken.split("\\.")[1];
        String noneHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String token = noneHeader + "." + payload + ".";

        assertAuthenticationFailure(tokenProvider::validateAccessToken, token, ErrorCode.UNAUTHORIZED);
    }

    @Test
    void Refresh를_Access_자리에_넣으면_거절한다() throws Exception {
        Instant now = Instant.now();
        String refreshToken = signedToken(
                ACCESS_SECRET,
                "speech-api",
                TokenType.REFRESH,
                now,
                now.plusSeconds(1_800)
        );

        assertAuthenticationFailure(
                tokenProvider::validateAccessToken,
                refreshToken,
                ErrorCode.UNAUTHORIZED
        );
    }

    @Test
    void 다른_audience로_서명한_토큰은_거절한다() throws Exception {
        Instant now = Instant.now();
        String token = signedToken(
                ACCESS_SECRET,
                "different-audience",
                TokenType.ACCESS,
                now,
                now.plusSeconds(1_800)
        );

        assertAuthenticationFailure(tokenProvider::validateAccessToken, token, ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 잘못된_Refresh는_전용_오류로_거절한다() {
        assertAuthenticationFailure(
                tokenProvider::validateRefreshToken,
                "not-a-jwt",
                ErrorCode.INVALID_REFRESH_TOKEN
        );
    }

    @Test
    void Access_비밀키가_32바이트보다_짧으면_거절한다() {
        assertThatThrownBy(() -> propertiesWithSecrets("short", REFRESH_SECRET))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void Refresh_비밀키가_32바이트보다_짧으면_거절한다() {
        assertThatThrownBy(() -> propertiesWithSecrets(ACCESS_SECRET, "short"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void Access와_Refresh_비밀키가_같으면_거절한다() {
        assertThatThrownBy(() -> propertiesWithSecrets(ACCESS_SECRET, ACCESS_SECRET))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 유효한_Access와_존재하는_사용자면_Long_principal을_설정한다() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.existsById(42L)).thenReturn(true);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userRepository);
        MockHttpServletRequest request = request("GET", "/users/me");
        request.addHeader("Authorization", "Bearer " + tokenProvider.issueAccessToken(42L));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(42L)
                .isInstanceOf(Long.class);
    }

    @Test
    void 유효한_Access라도_사용자가_없으면_principal을_설정하지_않는다() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.existsById(42L)).thenReturn(false);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userRepository);
        MockHttpServletRequest request = request("GET", "/users/me");
        request.addHeader("Authorization", "Bearer " + tokenProvider.issueAccessToken(42L));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void 인증_API는_만료된_Access가_붙어도_JWT_검사를_건너뛴다() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userRepository);
        MockHttpServletRequest request = request("POST", "/auth/login");
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked[0] = true);

        assertThat(chainInvoked[0]).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void 인증_API에_Origin이_없으면_403_envelope을_반환한다() throws Exception {
        OriginCheckFilter filter = new OriginCheckFilter(
                "http://localhost:5173",
                JsonMapper.builder().build()
        );
        MockHttpServletRequest request = request("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked[0] = true);

        assertThat(chainInvoked[0]).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEqualTo(
                "{\"success\":false,\"data\":null,\"error\":{\"code\":\"ORIGIN_NOT_ALLOWED\","
                        + "\"message\":\"허용되지 않은 요청입니다.\"}}"
        );
    }

    @Test
    void 인증_API에_허용된_Origin이면_요청을_통과시킨다() throws Exception {
        OriginCheckFilter filter = new OriginCheckFilter(
                "http://localhost:5173",
                JsonMapper.builder().build()
        );
        MockHttpServletRequest request = request("POST", "/auth/login");
        request.addHeader("Origin", "http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked[0] = true);

        assertThat(chainInvoked[0]).isTrue();
    }

    @Test
    void 인증_API의_Origin은_부분_일치나_서브도메인을_허용하지_않는다() throws Exception {
        OriginCheckFilter filter = new OriginCheckFilter(
                "http://localhost:5173",
                JsonMapper.builder().build()
        );
        MockHttpServletRequest request = request("POST", "/auth/login");
        request.addHeader("Origin", "http://localhost:5173.evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void 인증_API가_아닌_경로는_Origin이_없어도_통과시킨다() throws Exception {
        OriginCheckFilter filter = new OriginCheckFilter(
                "http://localhost:5173",
                JsonMapper.builder().build()
        );
        MockHttpServletRequest request = request("GET", "/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked[0] = true);

        assertThat(chainInvoked[0]).isTrue();
    }

    @Test
    void Refresh_발급_쿠키는_보안_속성과_인증_경로를_사용한다() {
        ResponseCookie cookie = new RefreshCookieFactory(properties).create("refresh-jwt");

        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEqualTo("refresh-jwt");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(1_209_600);
        assertThat(cookie.getDomain()).isNull();
    }

    @Test
    void Refresh_만료_쿠키는_같은_이름과_경로에서_즉시_만료된다() {
        ResponseCookie cookie = new RefreshCookieFactory(properties).expire();

        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge().isZero()).isTrue();
    }

    private String signedToken(
            String secret,
            String audience,
            TokenType tokenType,
            Instant issuedAt,
            Instant expiresAt
    ) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("speech-service")
                .audience(audience)
                .subject("42")
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .claim("tokenUse", tokenType.name())
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    private JwtProperties propertiesWithSecrets(String accessSecret, String refreshSecret) {
        return new JwtProperties(
                "speech-service",
                "speech-api",
                "speech-auth",
                1_800,
                1_209_600,
                accessSecret,
                refreshSecret
        );
    }

    private MockHttpServletRequest request(String method, String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, servletPath);
        request.setServletPath(servletPath);
        return request;
    }

    private void assertAuthenticationFailure(
            TokenValidator validator,
            String token,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }

    @FunctionalInterface
    private interface TokenValidator {
        Long validate(String token);
    }
}

@WebMvcTest(
        controllers = SecurityConfigWebTest.SecurityTestController.class,
        properties = {
                "jwt.issuer=speech-service",
                "jwt.access-audience=speech-api",
                "jwt.refresh-audience=speech-auth",
                "jwt.access-ttl-seconds=1800",
                "jwt.refresh-ttl-seconds=1209600",
                "jwt.access-secret=access-secret-that-is-at-least-32-bytes-long",
                "jwt.refresh-secret=refresh-secret-that-is-at-least-32-bytes-long",
                "app.allowed-origin=http://localhost:5173"
        }
)
@Import({
        SecurityConfig.class,
        JwtTokenProvider.class,
        NoStoreCacheFilter.class,
        SecurityConfigWebTest.SecurityTestController.class
})
class SecurityConfigWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void 보호_API를_토큰_없이_호출하면_401_envelope을_반환한다() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("로그인이 필요합니다."));
    }

    @Test
    void 로그인에_Origin이_없으면_403_envelope을_반환한다() throws Exception {
        mockMvc.perform(post("/auth/login").servletPath("/auth/login"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.error.code").value("ORIGIN_NOT_ALLOWED"));
    }

    @Test
    void 로그인에_허용된_Origin이_있으면_공개_경로로_통과한다() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .servletPath("/auth/login")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk());
    }

    @RestController
    static class SecurityTestController {

        @GetMapping("/users/me")
        String me() {
            return "protected";
        }

        @PostMapping("/auth/login")
        String login() {
            return "public";
        }
    }
}

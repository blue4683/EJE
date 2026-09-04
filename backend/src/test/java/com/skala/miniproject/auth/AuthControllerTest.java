package com.skala.miniproject.auth;

import com.nimbusds.jwt.SignedJWT;
import com.skala.miniproject.auth.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String ORIGIN = "http://localhost:5173";
    private static final String SIGN_UP_EMAIL = "a2-signup@example.com";
    private static final String INVALID_EMAIL = "a2-invalid@example.com";
    private static final String BOUNDARY_EMAIL = "a2-boundary@example.com";
    private static final String SPACED_PASSWORD_EMAIL = "a2-spaced@example.com";
    private static final String CONCURRENT_EMAIL = "a2-concurrent@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    @AfterEach
    void deleteTestUser() {
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", SIGN_UP_EMAIL);
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", INVALID_EMAIL);
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", BOUNDARY_EMAIL);
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", SPACED_PASSWORD_EMAIL);
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", CONCURRENT_EMAIL);
    }

    @Test
    void 회원가입은_사용자를_정규화해_저장하고_201과_Refresh_쿠키를_반환한다()
            throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .servletPath("/auth/signup")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "  A2-SignUp@Example.COM  ",
                                  "password": "P@ssw0rd123",
                                  "name": "  홍길동  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/users/me"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=1209600")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800))
                .andExpect(jsonPath("$.data.refreshExpiresAt").isNotEmpty())
                .andExpect(jsonPath("$.data.user.id").isString())
                .andExpect(jsonPath("$.data.user.email").value(SIGN_UP_EMAIL))
                .andExpect(jsonPath("$.data.user.name").value("홍길동"))
                .andExpect(jsonPath("$.data.user.profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.user.plan").value("FREE"))
                .andExpect(jsonPath("$.data.user.createdAt").isNotEmpty());

        Map<String, Object> savedUser = jdbcTemplate.queryForMap(
                "SELECT email, name, plan, profile_image_url FROM users WHERE email = ?",
                SIGN_UP_EMAIL
        );
        assertThat(savedUser)
                .containsEntry("email", SIGN_UP_EMAIL)
                .containsEntry("name", "홍길동")
                .containsEntry("plan", "FREE")
                .containsEntry("profile_image_url", null);
    }

    @Test
    void 회원가입의_refreshExpiresAt은_발급한_Refresh_JWT의_exp와_같다() throws Exception {
        MvcResult result = signUp(SIGN_UP_EMAIL, "P@ssw0rd123", "만료 사용자")
                .andExpect(status().isCreated())
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        String refreshToken = setCookie.substring(
                "refreshToken=".length(),
                setCookie.indexOf(';')
        );
        Instant jwtExpiration = SignedJWT.parse(refreshToken)
                .getJWTClaimsSet()
                .getExpirationTime()
                .toInstant();
        Instant responseExpiration = Instant.parse(jsonMapper.readTree(
                result.getResponse().getContentAsString()
                ).get("data").get("refreshExpiresAt").asString());

        assertThat(responseExpiration).isEqualTo(jwtExpiration);
    }

    @Test
    void 중복_이메일은_DB_UNIQUE_위반을_409로_변환한다() throws Exception {
        signUp(SIGN_UP_EMAIL, "P@ssw0rd123", "첫 사용자")
                .andExpect(status().isCreated());

        signUp("  A2-SIGNUP@EXAMPLE.COM  ", "An0therPass!", "두 번째 사용자")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.error.message").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    void 동시에_같은_이메일로_가입하면_DB_UNIQUE가_한_요청만_성공시킨다() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> concurrentSignUp(barrier, "첫 사용자"));
            Future<Integer> second = executor.submit(() -> concurrentSignUp(barrier, "두 번째 사용자"));

            assertThat(List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            ))
                    .containsExactlyInAnyOrder(201, 409);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE email = ?",
                Integer.class,
                CONCURRENT_EMAIL
        );
        assertThat(userCount).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidSignUps")
    void 회원가입_입력_규칙을_위반하면_422를_반환한다(
            String description,
            String email,
            String password,
            String name
    ) throws Exception {
        signUp(email, password, name)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE email = ?",
                Integer.class,
                INVALID_EMAIL
        );
        assertThat(userCount).isZero();
    }

    @Test
    void 회원가입_필수_필드가_누락되면_422를_반환한다() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .servletPath("/auth/signup")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a2-invalid@example.com\",\"name\":\"홍길동\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 로그인은_정규화한_이메일과_BCrypt_비밀번호를_검증하고_새_토큰을_반환한다()
            throws Exception {
        mockMvc.perform(post("/auth/login")
                        .servletPath("/auth/login")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"  USER@EXAMPLE.COM  ","password":"P@ssw0rd123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800))
                .andExpect(jsonPath("$.data.refreshExpiresAt").isNotEmpty())
                .andExpect(jsonPath("$.data.user.id").value("1"))
                .andExpect(jsonPath("$.data.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.user.plan").value("FREE"));
    }

    @Test
    void 재발급은_Refresh_쿠키만_검증해_Access만_반환한다() throws Exception {
        String refreshToken = tokenProvider.issueRefreshToken(1L);

        mockMvc.perform(post("/auth/reissue")
                        .servletPath("/auth/reissue")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800))
                .andExpect(jsonPath("$.data.refreshExpiresAt").doesNotExist())
                .andExpect(jsonPath("$.data.user").doesNotExist());
    }

    @Test
    void 잘못된_Refresh로_재발급하면_401과_만료_쿠키를_반환한다() throws Exception {
        mockMvc.perform(post("/auth/reissue")
                        .servletPath("/auth/reissue")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .cookie(new Cookie("refreshToken", "not-a-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void 로그아웃은_쿠키_유효성과_무관하게_만료_쿠키와_빈_204를_반환한다() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .servletPath("/auth/logout")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .cookie(new Cookie("refreshToken", "invalid-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(content().string(""));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validBoundaryPasswords")
    void 회원가입은_비밀번호_허용_경계를_받아들인다(String description, String password)
            throws Exception {
        signUp(BOUNDARY_EMAIL, password, "경계 사용자")
                .andExpect(status().isCreated());
    }

    @Test
    void 회원가입과_로그인은_비밀번호의_앞뒤_공백을_제거하지_않는다() throws Exception {
        String passwordWithSpaces = " P@ss word123 ";
        signUp(SPACED_PASSWORD_EMAIL, passwordWithSpaces, "공백 사용자")
                .andExpect(status().isCreated());

        login(SPACED_PASSWORD_EMAIL, passwordWithSpaces)
                .andExpect(status().isOk());
        login(SPACED_PASSWORD_EMAIL, passwordWithSpaces.trim())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCredentials")
    void 이메일이나_비밀번호가_틀리면_같은_로그인_오류를_반환한다(
            String description,
            String email,
            String password
    ) throws Exception {
        login(email, password)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message")
                        .value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void 로그인_비밀번호_입력_규칙을_위반하면_자격증명_오류가_아니라_422를_반환한다()
            throws Exception {
        login("user@example.com", "1234567")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void Refresh_쿠키가_없으면_401과_만료_쿠키를_반환한다() throws Exception {
        assertInvalidRefresh(null);
    }

    @Test
    void Access를_Refresh_쿠키에_넣으면_401과_만료_쿠키를_반환한다() throws Exception {
        assertInvalidRefresh(tokenProvider.issueAccessToken(1L));
    }

    @Test
    void 탈퇴한_사용자의_Refresh면_401과_만료_쿠키를_반환한다() throws Exception {
        assertInvalidRefresh(tokenProvider.issueRefreshToken(Long.MAX_VALUE));
    }

    @Test
    void 로그아웃은_쿠키가_없어도_204를_반환한다() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .servletPath("/auth/logout")
                        .header(HttpHeaders.ORIGIN, ORIGIN))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/auth/signup", "/auth/login", "/auth/reissue", "/auth/logout"})
    void 인증_API는_Origin이_없으면_모두_403을_반환한다(String endpoint) throws Exception {
        mockMvc.perform(post(endpoint).servletPath(endpoint))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ORIGIN_NOT_ALLOWED"));
    }

    @Test
    void 허용되지_않은_Origin의_인증_API는_ORIGIN_NOT_ALLOWED_envelope을_반환한다()
            throws Exception {
        mockMvc.perform(post("/auth/login")
                        .servletPath("/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"P@ssw0rd123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("ORIGIN_NOT_ALLOWED"));
    }

    @Test
    void 허용된_프론트엔드의_인증_API_preflight는_credentials_CORS_헤더를_반환한다()
            throws Exception {
        mockMvc.perform(options("/auth/login")
                        .servletPath("/auth/login")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")));
    }

    private static Stream<Arguments> invalidSignUps() {
        return Stream.of(
                Arguments.of("비ASCII 이메일", "사용자@example.com", "P@ssw0rd123", "홍길동"),
                Arguments.of("형식이 잘못된 이메일", "a2-invalid", "P@ssw0rd123", "홍길동"),
                Arguments.of("7 코드포인트 비밀번호", INVALID_EMAIL, "1234567", "홍길동"),
                Arguments.of(
                        "보조 평면 7 코드포인트 비밀번호",
                        INVALID_EMAIL,
                        "😀".repeat(7),
                        "홍길동"
                ),
                Arguments.of("65 코드포인트 비밀번호", INVALID_EMAIL, "a".repeat(65), "홍길동"),
                Arguments.of("UTF-8 72바이트 초과 비밀번호", INVALID_EMAIL, "가".repeat(25), "홍길동"),
                Arguments.of("공백 이름", INVALID_EMAIL, "P@ssw0rd123", "   "),
                Arguments.of("51 코드포인트 이름", INVALID_EMAIL, "P@ssw0rd123", "가".repeat(51))
        );
    }

    private static Stream<Arguments> validBoundaryPasswords() {
        return Stream.of(
                Arguments.of("8 코드포인트", "12345678"),
                Arguments.of("64 코드포인트", "a".repeat(64)),
                Arguments.of("보조 평면 포함 64 코드포인트", "a".repeat(63) + "😀"),
                Arguments.of("UTF-8 정확히 72바이트", "가".repeat(24))
        );
    }

    private static Stream<Arguments> invalidCredentials() {
        return Stream.of(
                Arguments.of("비밀번호 오류", "user@example.com", "WrongPass123!"),
                Arguments.of("존재하지 않는 이메일", "not-found@example.com", "P@ssw0rd123")
        );
    }

    private void assertInvalidRefresh(String refreshToken) throws Exception {
        var request = post("/auth/reissue")
                .servletPath("/auth/reissue")
                .header(HttpHeaders.ORIGIN, ORIGIN);
        if (refreshToken != null) {
            request.cookie(new Cookie("refreshToken", refreshToken));
        }

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    private int concurrentSignUp(CyclicBarrier barrier, String name) throws Exception {
        barrier.await();
        return signUp(CONCURRENT_EMAIL, "P@ssw0rd123", name)
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .servletPath("/auth/login")
                .header(HttpHeaders.ORIGIN, ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)));
    }

    private org.springframework.test.web.servlet.ResultActions signUp(
            String email,
            String password,
            String name
    ) throws Exception {
        return mockMvc.perform(post("/auth/signup")
                .servletPath("/auth/signup")
                .header(HttpHeaders.ORIGIN, ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","name":"%s"}
                        """.formatted(email, password, name)));
    }
}

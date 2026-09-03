package com.skala.miniproject.auth.controller;

import com.skala.miniproject.auth.dto.AccessData;
import com.skala.miniproject.auth.dto.LoginData;
import com.skala.miniproject.auth.dto.LoginRequest;
import com.skala.miniproject.auth.dto.SignUpRequest;
import com.skala.miniproject.auth.jwt.RefreshCookieFactory;
import com.skala.miniproject.auth.service.AuthService;
import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<LoginData>> signUp(@RequestBody SignUpRequest request) {
        AuthService.LoginResult result = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/users/me"))
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.create(result.refreshToken()).toString())
                .body(ApiResponse.ok(result.data()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginData>> login(@RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.create(result.refreshToken()).toString())
                .body(ApiResponse.ok(result.data()));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<AccessData>> reissue(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(authService.reissue(refreshToken)));
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.INVALID_REFRESH_TOKEN) {
                response.addHeader(HttpHeaders.SET_COOKIE, refreshCookieFactory.expire().toString());
            }
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.expire().toString())
                .build();
    }
}

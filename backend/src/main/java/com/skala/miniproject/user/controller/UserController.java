package com.skala.miniproject.user.controller;

import com.skala.miniproject.auth.dto.UserDto;
import com.skala.miniproject.auth.jwt.RefreshCookieFactory;
import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.common.security.CurrentUser;
import com.skala.miniproject.user.dto.WithdrawRequest;
import com.skala.miniproject.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RefreshCookieFactory refreshCookieFactory;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getMe() {
        Long userId = CurrentUser.id();
        return ResponseEntity.ok(ApiResponse.ok(userService.getMe(userId)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@RequestBody WithdrawRequest request) {
        Long userId = CurrentUser.id();
        userService.withdraw(userId, request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.expire().toString())
                .build();
    }
}

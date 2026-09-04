package com.skala.miniproject.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * A1(JWT 필터) 없이 CurrentUser.id() 가 동작하도록 SecurityContextHolder 에 Long principal 을
 * 직접 세팅한다. JwtAuthenticationFilter 가 인증 성공 시 만드는 것과 같은 형태의 인증 객체를 쓴다.
 */
public final class TestSecurityContext {

    private TestSecurityContext() {
    }

    public static void loginAs(Long userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}

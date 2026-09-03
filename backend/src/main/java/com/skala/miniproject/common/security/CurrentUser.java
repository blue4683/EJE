package com.skala.miniproject.common.security;

import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 컨트롤러가 현재 사용자 ID 를 얻는 유일한 통로.
 *
 * C1 — SecurityContextHolder 는 ThreadLocal 이다. @Async·가상 스레드로 넘어가면 컨텍스트가 없다.
 *      이 메서드는 컨트롤러 계층에서만 호출한다. 비동기 진입점은 userId 를 인자로 받는다.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}

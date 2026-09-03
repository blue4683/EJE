package com.skala.miniproject.auth.jwt;

import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.domain.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> AUTH_PATHS = Set.of(
            "/auth/signup",
            "/auth/login",
            "/auth/reissue",
            "/auth/logout"
    );

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return AUTH_PATHS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Long userId = tokenProvider.validateAccessToken(authorization.substring(BEARER_PREFIX.length()));
            if (userRepository.existsById(userId)) {
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (BusinessException e) {
            // 실패한 토큰은 인증 정보로 사용하지 않는다.
            // 이후 인가 단계가 다른 무토큰 요청과 동일한 401 envelope을 만든다.
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}

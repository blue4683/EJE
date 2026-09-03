package com.skala.miniproject.auth.jwt;

import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class OriginCheckFilter extends OncePerRequestFilter {

    private static final Set<String> AUTH_PATHS = Set.of(
            "/auth/signup",
            "/auth/login",
            "/auth/reissue",
            "/auth/logout"
    );

    private final String allowedOrigin;
    private final JsonMapper jsonMapper;

    public OriginCheckFilter(String allowedOrigin, JsonMapper jsonMapper) {
        this.allowedOrigin = allowedOrigin;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !AUTH_PATHS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        if (!allowedOrigin.equals(request.getHeader("Origin"))) {
            writeError(response, ErrorCode.ORIGIN_NOT_ALLOWED);
            return;
        }
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(
                response.getWriter(),
                ApiResponse.fail(errorCode.name(), errorCode.getMessage())
        );
    }
}

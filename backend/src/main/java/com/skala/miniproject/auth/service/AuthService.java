package com.skala.miniproject.auth.service;

import com.skala.miniproject.auth.dto.AccessData;
import com.skala.miniproject.auth.dto.LoginData;
import com.skala.miniproject.auth.dto.LoginRequest;
import com.skala.miniproject.auth.dto.SignUpRequest;
import com.skala.miniproject.auth.dto.UserDto;
import com.skala.miniproject.auth.jwt.JwtTokenProvider;
import com.skala.miniproject.auth.repository.AuthUserQueryRepository;
import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.config.JwtProperties;
import com.skala.miniproject.domain.user.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$12$ZgH8t9.skqbuAYP1/ADm6.a3A3gUOdCWDkk9Zq2xt/Ju6KJOc6YHa";
    private static final Pattern ASCII_EMAIL = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
                    + "@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$"
    );

    private final AuthUserQueryRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final TransactionTemplate transactionTemplate;

    public AuthService(
            AuthUserQueryRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            JwtProperties jwtProperties,
            PlatformTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public LoginResult signUp(SignUpRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        validatePassword(request.password());
        String normalizedName = normalizeName(request.name());
        String passwordHash = passwordEncoder.encode(request.password());

        User user;
        try {
            user = Objects.requireNonNull(transactionTemplate.execute(status -> userRepository.save(
                    User.signUp(normalizedEmail, passwordHash, normalizedName, Instant.now())
            )));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        return issueLoginResult(user);
    }

    public LoginResult login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        validatePassword(request.password());

        Optional<User> user = userRepository.findByEmail(normalizedEmail);
        String passwordHash = user.map(User::getPasswordHash).orElse(DUMMY_PASSWORD_HASH);
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
        if (user.isEmpty() || !passwordMatches) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueLoginResult(user.orElseThrow());
    }

    public AccessData reissue(String refreshToken) {
        Long userId = tokenProvider.validateRefreshToken(refreshToken);
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return new AccessData(
                tokenProvider.issueAccessToken(userId),
                TOKEN_TYPE,
                jwtProperties.accessTtlSeconds()
        );
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw validationError();
        }
        String trimmedEmail = email.trim();
        if (trimmedEmail.codePointCount(0, trimmedEmail.length()) > 254
                || !ASCII_EMAIL.matcher(trimmedEmail).matches()) {
            throw validationError();
        }
        return trimmedEmail.toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (password == null) {
            throw validationError();
        }
        int codePointCount = password.codePointCount(0, password.length());
        if (codePointCount < 8
                || codePointCount > 64
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw validationError();
        }
    }

    private String normalizeName(String name) {
        if (name == null) {
            throw validationError();
        }
        String trimmedName = name.trim();
        int codePointCount = trimmedName.codePointCount(0, trimmedName.length());
        if (codePointCount < 1 || codePointCount > 50) {
            throw validationError();
        }
        return trimmedName;
    }

    private BusinessException validationError() {
        return new BusinessException(ErrorCode.VALIDATION_ERROR);
    }

    private LoginResult issueLoginResult(User user) {
        String accessToken = tokenProvider.issueAccessToken(user.getId());
        JwtTokenProvider.IssuedToken refreshToken = tokenProvider.issueRefreshTokenWithExpiration(user.getId());
        LoginData data = new LoginData(
                accessToken,
                TOKEN_TYPE,
                jwtProperties.accessTtlSeconds(),
                refreshToken.expiresAt(),
                UserDto.from(user)
        );
        return new LoginResult(data, refreshToken.value());
    }

    public record LoginResult(LoginData data, String refreshToken) {
    }
}

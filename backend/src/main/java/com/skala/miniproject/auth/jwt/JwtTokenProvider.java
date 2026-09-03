package com.skala.miniproject.auth.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.config.JwtProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class JwtTokenProvider {

    private static final Pattern POSITIVE_LONG = Pattern.compile("[1-9][0-9]*");

    private final JwtProperties properties;
    private final byte[] accessSecret;
    private final byte[] refreshSecret;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.accessSecret = properties.accessSecret().getBytes(StandardCharsets.UTF_8);
        this.refreshSecret = properties.refreshSecret().getBytes(StandardCharsets.UTF_8);
    }

    public String issueAccessToken(Long userId) {
        return issue(userId, TokenType.ACCESS).value();
    }

    public String issueRefreshToken(Long userId) {
        return issueRefreshTokenWithExpiration(userId).value();
    }

    public IssuedToken issueRefreshTokenWithExpiration(Long userId) {
        return issue(userId, TokenType.REFRESH);
    }

    public Long validateAccessToken(String token) {
        return validate(token, TokenType.ACCESS);
    }

    public Long validateRefreshToken(String token) {
        return validate(token, TokenType.REFRESH);
    }

    private IssuedToken issue(Long userId, TokenType tokenType) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
        }

        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plusSeconds(ttlSeconds(tokenType));
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.issuer())
                .audience(audience(tokenType))
                .subject(userId.toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .claim("tokenUse", tokenType.name())
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);

        try {
            jwt.sign(new MACSigner(secret(tokenType)));
            return new IssuedToken(jwt.serialize(), expiresAt);
        } catch (JOSEException e) {
            throw new IllegalStateException("JWT 서명에 실패했습니다.", e);
        }
    }

    private Long validate(String token, TokenType tokenType) {
        ErrorCode failureCode = failureCode(tokenType);

        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
                throw new BusinessException(failureCode);
            }
            if (!jwt.verify(new MACVerifier(secret(tokenType)))) {
                throw new BusinessException(failureCode);
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Instant now = Instant.now();
            Date issuedAt = claims.getIssueTime();
            Date expiresAt = claims.getExpirationTime();

            if (!properties.issuer().equals(claims.getIssuer())
                    || !List.of(audience(tokenType)).equals(claims.getAudience())
                    || !tokenType.name().equals(claims.getStringClaim("tokenUse"))
                    || issuedAt == null
                    || expiresAt == null) {
                throw new BusinessException(failureCode);
            }

            Instant issuedInstant = issuedAt.toInstant();
            Instant expiresInstant = expiresAt.toInstant();
            if (issuedInstant.isAfter(now)
                    || !now.isBefore(expiresInstant)
                    || !expiresInstant.isAfter(issuedInstant)
                    || Duration.between(issuedInstant, expiresInstant).getSeconds() > ttlSeconds(tokenType)) {
                throw new BusinessException(failureCode);
            }

            return parseSubject(claims.getSubject(), failureCode);
        } catch (ParseException | JOSEException | RuntimeException e) {
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(failureCode);
        }
    }

    private Long parseSubject(String subject, ErrorCode failureCode) {
        if (subject == null || !POSITIVE_LONG.matcher(subject).matches()) {
            throw new BusinessException(failureCode);
        }
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new BusinessException(failureCode);
        }
    }

    private byte[] secret(TokenType tokenType) {
        return tokenType == TokenType.ACCESS ? accessSecret : refreshSecret;
    }

    private String audience(TokenType tokenType) {
        return tokenType == TokenType.ACCESS
                ? properties.accessAudience()
                : properties.refreshAudience();
    }

    private long ttlSeconds(TokenType tokenType) {
        return tokenType == TokenType.ACCESS
                ? properties.accessTtlSeconds()
                : properties.refreshTtlSeconds();
    }

    private ErrorCode failureCode(TokenType tokenType) {
        return tokenType == TokenType.ACCESS
                ? ErrorCode.UNAUTHORIZED
                : ErrorCode.INVALID_REFRESH_TOKEN;
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}

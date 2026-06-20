package kuan.fintech.fintech_auth_service.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import kuan.fintech.fintech_auth_service.application.result.TokenPair;
import kuan.fintech.fintech_auth_service.domain.error.AuthErrorCode;
import kuan.fintech.fintech_auth_service.domain.error.AuthException;
import kuan.fintech.fintech_auth_service.domain.model.UserStatus;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.entity.AuthUserEntity;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.entity.RefreshTokenEntity;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import kuan.fintech.fintech_auth_service.infrastructure.security.JwtTokenProvider;
import kuan.fintech.fintech_auth_service.infrastructure.security.JwtProperties;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final int REFRESH_TOKEN_BYTES = 48;

    private final RefreshTokenJpaRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(
            RefreshTokenJpaRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    public TokenPair issueTokenPair(AuthUserEntity user, String ipAddress, String userAgent) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = issueRefreshToken(user, ipAddress, userAgent, Instant.now());

        return new TokenPair(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenProvider.accessTokenTtlSeconds());
    }

    public TokenPair rotateRefreshToken(String rawRefreshToken, String ipAddress, String userAgent) {
        Instant now = Instant.now();
        RefreshTokenEntity existingToken = resolveUsableRefreshToken(rawRefreshToken, now);
        AuthUserEntity user = existingToken.getUser();

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException(AuthErrorCode.USER_DISABLED);
        }

        existingToken.revoke(now);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = issueRefreshToken(user, ipAddress, userAgent, now);

        return new TokenPair(
                accessToken,
                newRefreshToken,
                "Bearer",
                jwtTokenProvider.accessTokenTtlSeconds());
    }

    public void revokeRefreshToken(String rawRefreshToken) {
        RefreshTokenEntity token = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID));

        if (!token.isRevoked()) {
            token.revoke(Instant.now());
        }
    }

    private RefreshTokenEntity resolveUsableRefreshToken(String rawRefreshToken, Instant now) {
        RefreshTokenEntity token = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID));

        if (token.isRevoked()) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_REVOKED);
        }
        if (token.isExpired(now)) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        return token;
    }

    private String issueRefreshToken(AuthUserEntity user, String ipAddress, String userAgent, Instant now) {
        String rawToken = randomRefreshToken();
        refreshTokenRepository.save(new RefreshTokenEntity(
                UUID.randomUUID(),
                user,
                hash(rawToken),
                now.plus(jwtProperties.refreshTokenTtl()),
                now,
                ipAddress,
                userAgent));
        return rawToken;
    }

    private String randomRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}

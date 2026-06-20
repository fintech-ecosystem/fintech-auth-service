package kuan.fintech.fintech_auth_service.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays) {

    public Duration accessTokenTtl() {
        return Duration.ofMinutes(accessTokenTtlMinutes);
    }

    public Duration refreshTokenTtl() {
        return Duration.ofDays(refreshTokenTtlDays);
    }
}

package kuan.fintech.fintech_auth_service.infrastructure.security;

import java.time.Instant;
import java.util.List;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.entity.AuthUserEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties properties;

    public JwtTokenProvider(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.properties = properties;
    }

    public String generateAccessToken(AuthUserEntity user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .sorted()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    public long refreshTokenTtlSeconds() {
        return properties.refreshTokenTtl().toSeconds();
    }
}

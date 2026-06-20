package kuan.fintech.fintech_auth_service.application.result;

public record TokenPair(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}

package kuan.fintech.fintech_auth_service.application.command;

public record RefreshTokenCommand(String refreshToken, String ipAddress, String userAgent) {
}

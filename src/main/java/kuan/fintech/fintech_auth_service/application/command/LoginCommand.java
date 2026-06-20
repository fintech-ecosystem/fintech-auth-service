package kuan.fintech.fintech_auth_service.application.command;

public record LoginCommand(String email, String password, String ipAddress, String userAgent) {
}

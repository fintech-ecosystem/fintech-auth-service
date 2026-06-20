package kuan.fintech.fintech_auth_service.application.result;

import java.util.UUID;

public record LoginResult(UUID userId, String email, TokenPair tokens) {
}

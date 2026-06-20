package kuan.fintech.fintech_auth_service.interfaces.rest.response;

import java.util.UUID;

public record RegisterResponse(UUID userId, String email, String status) {
}

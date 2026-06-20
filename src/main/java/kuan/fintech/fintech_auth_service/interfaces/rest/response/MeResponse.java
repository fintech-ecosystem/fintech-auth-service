package kuan.fintech.fintech_auth_service.interfaces.rest.response;

import java.util.List;
import java.util.UUID;

public record MeResponse(UUID userId, String email, List<String> roles) {
}

package kuan.fintech.fintech_auth_service.application.result;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, List<String> roles) {
}

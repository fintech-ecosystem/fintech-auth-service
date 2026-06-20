package kuan.fintech.fintech_auth_service.application.result;

import java.util.UUID;
import kuan.fintech.fintech_auth_service.domain.model.UserStatus;

public record RegisterResult(UUID userId, String email, UserStatus status) {
}

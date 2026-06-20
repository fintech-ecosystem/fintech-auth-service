package kuan.fintech.fintech_auth_service.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank String refreshToken) {
}

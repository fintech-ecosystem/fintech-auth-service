package kuan.fintech.fintech_auth_service.interfaces.rest.response;

import kuan.fintech.fintech_auth_service.application.result.TokenPair;

public record LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

    public static LoginResponse from(TokenPair tokenPair) {
        return new LoginResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.tokenType(),
                tokenPair.expiresIn());
    }
}

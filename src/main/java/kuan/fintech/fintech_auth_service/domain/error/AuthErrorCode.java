package kuan.fintech.fintech_auth_service.domain.error;

import org.springframework.http.HttpStatus;

public enum AuthErrorCode {
    EMAIL_ALREADY_EXISTS("AUTH_EMAIL_ALREADY_EXISTS", "Email already exists", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED),
    USER_DISABLED("AUTH_USER_DISABLED", "User is disabled", HttpStatus.FORBIDDEN),
    REFRESH_TOKEN_INVALID("AUTH_REFRESH_TOKEN_INVALID", "Refresh token is invalid", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("AUTH_REFRESH_TOKEN_EXPIRED", "Refresh token is expired", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REVOKED("AUTH_REFRESH_TOKEN_REVOKED", "Refresh token has been revoked", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    AuthErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}

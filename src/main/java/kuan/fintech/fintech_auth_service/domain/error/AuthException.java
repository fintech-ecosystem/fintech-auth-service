package kuan.fintech.fintech_auth_service.domain.error;

public class AuthException extends RuntimeException {

    private final AuthErrorCode errorCode;

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public AuthErrorCode errorCode() {
        return errorCode;
    }
}

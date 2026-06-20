package kuan.fintech.fintech_auth_service.interfaces.rest;

import kuan.fintech.common.api.ApiError;
import kuan.fintech.common.api.ApiResponse;
import kuan.fintech.fintech_auth_service.domain.error.AuthErrorCode;
import kuan.fintech.fintech_auth_service.domain.error.AuthException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException exception) {
        AuthErrorCode code = exception.errorCode();
        ApiError error = ApiError.of(code.code(), exception.getMessage());
        return ResponseEntity.status(code.httpStatus()).body(ApiResponse.failure(error));
    }
}

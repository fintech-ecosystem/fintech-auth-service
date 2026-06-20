package kuan.fintech.fintech_auth_service.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kuan.fintech.common.api.ApiResponse;
import kuan.fintech.fintech_auth_service.application.command.LoginCommand;
import kuan.fintech.fintech_auth_service.application.command.LogoutCommand;
import kuan.fintech.fintech_auth_service.application.command.RefreshTokenCommand;
import kuan.fintech.fintech_auth_service.application.command.RegisterCommand;
import kuan.fintech.fintech_auth_service.application.result.AuthenticatedUser;
import kuan.fintech.fintech_auth_service.application.result.LoginResult;
import kuan.fintech.fintech_auth_service.application.result.RegisterResult;
import kuan.fintech.fintech_auth_service.application.result.TokenPair;
import kuan.fintech.fintech_auth_service.application.service.AuthApplicationService;
import kuan.fintech.fintech_auth_service.interfaces.rest.request.LoginRequest;
import kuan.fintech.fintech_auth_service.interfaces.rest.request.LogoutRequest;
import kuan.fintech.fintech_auth_service.interfaces.rest.request.RefreshTokenRequest;
import kuan.fintech.fintech_auth_service.interfaces.rest.request.RegisterRequest;
import kuan.fintech.fintech_auth_service.interfaces.rest.response.LoginResponse;
import kuan.fintech.fintech_auth_service.interfaces.rest.response.LogoutResponse;
import kuan.fintech.fintech_auth_service.interfaces.rest.response.MeResponse;
import kuan.fintech.fintech_auth_service.interfaces.rest.response.RegisterResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthApplicationService authService;

    public AuthController(AuthApplicationService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResult result = authService.register(new RegisterCommand(request.email(), request.password()));
        return ApiResponse.success(new RegisterResponse(
                result.userId(),
                result.email(),
                result.status().name()));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        LoginResult result = authService.login(new LoginCommand(
                request.email(),
                request.password(),
                clientIp(servletRequest),
                userAgent(servletRequest)));

        return ApiResponse.success(LoginResponse.from(result.tokens()));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest) {
        TokenPair tokenPair = authService.refresh(new RefreshTokenCommand(
                request.refreshToken(),
                clientIp(servletRequest),
                userAgent(servletRequest)));

        return ApiResponse.success(LoginResponse.from(tokenPair));
    }

    @PostMapping("/logout")
    public ApiResponse<LogoutResponse> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(new LogoutCommand(request.refreshToken()));
        return ApiResponse.success(new LogoutResponse(true));
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser user = authService.me(jwt);
        return ApiResponse.success(new MeResponse(user.userId(), user.email(), user.roles()));
    }

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("fintech-auth-service is running");
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}

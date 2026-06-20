package kuan.fintech.fintech_auth_service.application.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kuan.fintech.fintech_auth_service.application.command.LoginCommand;
import kuan.fintech.fintech_auth_service.application.command.LogoutCommand;
import kuan.fintech.fintech_auth_service.application.command.RefreshTokenCommand;
import kuan.fintech.fintech_auth_service.application.command.RegisterCommand;
import kuan.fintech.fintech_auth_service.application.result.AuthenticatedUser;
import kuan.fintech.fintech_auth_service.application.result.LoginResult;
import kuan.fintech.fintech_auth_service.application.result.RegisterResult;
import kuan.fintech.fintech_auth_service.application.result.TokenPair;
import kuan.fintech.fintech_auth_service.domain.error.AuthErrorCode;
import kuan.fintech.fintech_auth_service.domain.error.AuthException;
import kuan.fintech.fintech_auth_service.domain.model.Role;
import kuan.fintech.fintech_auth_service.domain.model.UserStatus;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.entity.AuthUserEntity;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.entity.RoleEntity;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.repository.AuthUserJpaRepository;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.repository.RoleJpaRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {

    private final AuthUserJpaRepository authUserRepository;
    private final RoleJpaRepository roleRepository;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttemptService;

    public AuthApplicationService(
            AuthUserJpaRepository authUserRepository,
            RoleJpaRepository roleRepository,
            PasswordService passwordService,
            TokenService tokenService,
            LoginAttemptService loginAttemptService) {
        this.authUserRepository = authUserRepository;
        this.roleRepository = roleRepository;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public RegisterResult register(RegisterCommand command) {
        String normalizedEmail = normalizeEmail(command.email());

        if (authUserRepository.existsByEmail(normalizedEmail)) {
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        RoleEntity customerRole = roleRepository.findByName(Role.CUSTOMER.name())
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role not found"));

        Instant now = Instant.now();
        AuthUserEntity user = new AuthUserEntity(
                UUID.randomUUID(),
                normalizedEmail,
                passwordService.hash(command.password()),
                UserStatus.ACTIVE,
                Set.of(customerRole),
                now,
                now);

        AuthUserEntity savedUser = authUserRepository.save(user);
        return new RegisterResult(savedUser.getId(), savedUser.getEmail(), savedUser.getStatus());
    }

    @Transactional
    public LoginResult login(LoginCommand command) {
        String normalizedEmail = normalizeEmail(command.email());

        try {
            AuthUserEntity user = authUserRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

            if (!passwordService.matches(command.password(), user.getPasswordHash())) {
                throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
            }

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new AuthException(AuthErrorCode.USER_DISABLED);
            }

            TokenPair tokens = tokenService.issueTokenPair(user, command.ipAddress(), command.userAgent());
            loginAttemptService.record(normalizedEmail, true, null, command.ipAddress(), command.userAgent());

            return new LoginResult(user.getId(), user.getEmail(), tokens);
        }
        catch (AuthException ex) {
            loginAttemptService.record(
                    normalizedEmail,
                    false,
                    ex.errorCode().code(),
                    command.ipAddress(),
                    command.userAgent());
            throw ex;
        }
    }

    @Transactional
    public TokenPair refresh(RefreshTokenCommand command) {
        return tokenService.rotateRefreshToken(command.refreshToken(), command.ipAddress(), command.userAgent());
    }

    @Transactional
    public void logout(LogoutCommand command) {
        tokenService.revokeRefreshToken(command.refreshToken());
    }

    public AuthenticatedUser me(Jwt jwt) {
        return new AuthenticatedUser(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                rolesFrom(jwt));
    }

    private List<String> rolesFrom(Jwt jwt) {
        Object roles = jwt.getClaims().get("roles");
        if (roles instanceof Collection<?> collection) {
            return collection.stream()
                    .map(String::valueOf)
                    .toList();
        }
        return List.of();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}

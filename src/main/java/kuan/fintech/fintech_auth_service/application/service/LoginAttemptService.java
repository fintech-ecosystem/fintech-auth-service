package kuan.fintech.fintech_auth_service.application.service;

import java.time.Instant;
import java.util.UUID;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.entity.LoginAttemptEntity;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.repository.LoginAttemptJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {

    private final LoginAttemptJpaRepository loginAttemptRepository;

    public LoginAttemptService(LoginAttemptJpaRepository loginAttemptRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String email, boolean success, String failureReason, String ipAddress, String userAgent) {
        loginAttemptRepository.save(new LoginAttemptEntity(
                UUID.randomUUID(),
                email,
                success,
                failureReason,
                ipAddress,
                userAgent,
                Instant.now()));
    }
}

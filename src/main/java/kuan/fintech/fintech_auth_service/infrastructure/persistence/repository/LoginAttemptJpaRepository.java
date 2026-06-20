package kuan.fintech.fintech_auth_service.infrastructure.persistence.repository;

import java.util.UUID;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.entity.LoginAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptJpaRepository extends JpaRepository<LoginAttemptEntity, UUID> {
}

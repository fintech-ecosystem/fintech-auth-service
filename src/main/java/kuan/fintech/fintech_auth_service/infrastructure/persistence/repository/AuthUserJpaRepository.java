package kuan.fintech.fintech_auth_service.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.entity.AuthUserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserJpaRepository extends JpaRepository<AuthUserEntity, UUID> {

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<AuthUserEntity> findByEmail(String email);
}

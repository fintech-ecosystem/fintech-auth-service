package kuan.fintech.fintech_auth_service.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
}

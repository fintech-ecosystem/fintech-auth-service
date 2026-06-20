package kuan.fintech.fintech_auth_service.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByName(String name);
}

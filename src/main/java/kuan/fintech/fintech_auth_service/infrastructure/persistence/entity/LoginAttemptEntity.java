package kuan.fintech.fintech_auth_service.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "login_attempts")
public class LoginAttemptEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LoginAttemptEntity() {
    }

    public LoginAttemptEntity(
            UUID id,
            String email,
            boolean success,
            String failureReason,
            String ipAddress,
            String userAgent,
            Instant createdAt) {
        this.id = id;
        this.email = email;
        this.success = success;
        this.failureReason = failureReason;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
    }
}

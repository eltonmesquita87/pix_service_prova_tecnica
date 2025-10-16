package com.elton.pixservice.infrastructure.persistence.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys", uniqueConstraints = {
    @UniqueConstraint(name = "uk_scope_key", columnNames = {"scope", "key_value"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKeyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String scope;

    @Column(name = "key_value", nullable = false, length = 255)
    private String keyValue;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String response;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

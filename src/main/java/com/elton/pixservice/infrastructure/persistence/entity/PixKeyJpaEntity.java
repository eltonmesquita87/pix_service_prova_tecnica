package com.elton.pixservice.infrastructure.persistence.entity;

import com.elton.pixservice.domain.valueobject.PixKeyType;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pix_keys", uniqueConstraints = {
    @UniqueConstraint(name = "uk_pix_key_value", columnNames = "key_value")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixKeyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false, length = 20)
    private PixKeyType keyType;

    @Column(name = "key_value", nullable = false, unique = true, length = 255)
    private String keyValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

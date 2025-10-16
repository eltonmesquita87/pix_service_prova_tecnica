package com.elton.pixservice.infrastructure.persistence.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events", uniqueConstraints = {
    @UniqueConstraint(name = "uk_event_id", columnNames = "event_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "end_to_end_id", nullable = false, length = 100)
    private String endToEndId;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        processedAt = LocalDateTime.now();
    }
}

package com.elton.pixservice.infrastructure.persistence.repository;

import com.elton.pixservice.infrastructure.persistence.entity.WebhookEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookEventJpaRepository extends JpaRepository<WebhookEventJpaEntity, Long> {
    boolean existsByEventId(String eventId);
}

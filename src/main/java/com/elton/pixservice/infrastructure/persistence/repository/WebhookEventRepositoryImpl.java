package com.elton.pixservice.infrastructure.persistence.repository;

import com.elton.pixservice.domain.repository.WebhookEventRepository;
import com.elton.pixservice.infrastructure.persistence.entity.WebhookEventJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WebhookEventRepositoryImpl implements WebhookEventRepository {

    private final WebhookEventJpaRepository jpaRepository;

    @Override
    @Transactional
    public void saveEvent(String eventId, String endToEndId, String eventType) {
        WebhookEventJpaEntity entity = WebhookEventJpaEntity.builder()
                .eventId(eventId)
                .endToEndId(endToEndId)
                .eventType(eventType)
                .build();
        jpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean eventAlreadyProcessed(String eventId) {
        return jpaRepository.existsByEventId(eventId);
    }
}

package com.elton.pixservice.domain.repository;

/**
 * Repository interface for webhook event tracking (Port).
 */
public interface WebhookEventRepository {
    void saveEvent(String eventId, String endToEndId, String eventType);
    boolean eventAlreadyProcessed(String eventId);
}

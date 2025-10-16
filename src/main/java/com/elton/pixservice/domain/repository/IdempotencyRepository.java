package com.elton.pixservice.domain.repository;

import java.util.Optional;

/**
 * Repository interface for idempotency control (Port).
 */
public interface IdempotencyRepository {
    void saveIdempotencyKey(String scope, String key, String response);
    Optional<String> findResponse(String scope, String key);
    boolean exists(String scope, String key);
}

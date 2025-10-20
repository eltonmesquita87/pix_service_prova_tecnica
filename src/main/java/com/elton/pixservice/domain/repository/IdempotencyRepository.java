package com.elton.pixservice.domain.repository;

import java.util.Optional;

/**
 * Repository interface for idempotency control (Port).
 */
public interface IdempotencyRepository {
    void saveIdempotencyKey(String scope, String key, String response);
    boolean exists(String scope, String key);
}

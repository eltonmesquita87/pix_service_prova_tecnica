package com.elton.pixservice.infrastructure.persistence.repository;

import com.elton.pixservice.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyJpaEntity, Long> {
    Optional<IdempotencyKeyJpaEntity> findByScopeAndKeyValue(String scope, String keyValue);
    boolean existsByScopeAndKeyValue(String scope, String keyValue);
}

package com.elton.pixservice.infrastructure.persistence.repository;

import com.elton.pixservice.domain.repository.IdempotencyRepository;
import com.elton.pixservice.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdempotencyRepositoryImpl implements IdempotencyRepository {

    private final IdempotencyKeyJpaRepository jpaRepository;

    @Override
    @Transactional
    public void saveIdempotencyKey(String scope, String key, String response) {
        IdempotencyKeyJpaEntity entity = IdempotencyKeyJpaEntity.builder()
                .scope(scope)
                .keyValue(key)
                .response(response)
                .build();
        jpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findResponse(String scope, String key) {
        return jpaRepository.findByScopeAndKeyValue(scope, key)
                .map(IdempotencyKeyJpaEntity::getResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String scope, String key) {
        return jpaRepository.existsByScopeAndKeyValue(scope, key);
    }
}

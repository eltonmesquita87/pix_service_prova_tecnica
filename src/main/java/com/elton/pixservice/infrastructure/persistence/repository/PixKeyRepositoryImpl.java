package com.elton.pixservice.infrastructure.persistence.repository;

import com.elton.pixservice.domain.entity.PixKey;
import com.elton.pixservice.domain.repository.PixKeyRepository;
import com.elton.pixservice.infrastructure.persistence.entity.PixKeyJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PixKeyRepositoryImpl implements PixKeyRepository {

    private final PixKeyJpaRepository jpaRepository;

    @Override
    @Transactional
    public PixKey save(PixKey pixKey) {
        PixKeyJpaEntity entity = toJpaEntity(pixKey);
        PixKeyJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PixKey> findByKeyValue(String keyValue) {
        return jpaRepository.findByKeyValue(keyValue).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PixKey> findByWalletId(Long walletId) {
        return jpaRepository.findByWalletId(walletId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByKeyValue(String keyValue) {
        return jpaRepository.existsByKeyValue(keyValue);
    }

    private PixKeyJpaEntity toJpaEntity(PixKey pixKey) {
        return PixKeyJpaEntity.builder()
                .id(pixKey.getId())
                .walletId(pixKey.getWalletId())
                .keyType(pixKey.getKeyType())
                .keyValue(pixKey.getKeyValue())
                .createdAt(pixKey.getCreatedAt())
                .build();
    }

    private PixKey toDomain(PixKeyJpaEntity entity) {
        return PixKey.builder()
                .id(entity.getId())
                .walletId(entity.getWalletId())
                .keyType(entity.getKeyType())
                .keyValue(entity.getKeyValue())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

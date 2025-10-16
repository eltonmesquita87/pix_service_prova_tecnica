package com.elton.pixservice.infrastructure.persistence.repository;

import com.elton.pixservice.domain.entity.PixTransfer;
import com.elton.pixservice.domain.repository.PixTransferRepository;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.infrastructure.persistence.entity.PixTransferJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PixTransferRepositoryImpl implements PixTransferRepository {

    private final PixTransferJpaRepository jpaRepository;

    @Override
    @Transactional
    public PixTransfer save(PixTransfer pixTransfer) {
        PixTransferJpaEntity entity = toJpaEntity(pixTransfer);
        PixTransferJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PixTransfer> findByEndToEndId(String endToEndId) {
        return jpaRepository.findByEndToEndId(endToEndId).map(this::toDomain);
    }

    @Override
    @Transactional
    public Optional<PixTransfer> findByEndToEndIdWithLock(String endToEndId) {
        return jpaRepository.findByEndToEndIdWithLock(endToEndId).map(this::toDomain);
    }

    private PixTransferJpaEntity toJpaEntity(PixTransfer pixTransfer) {
        return PixTransferJpaEntity.builder()
                .endToEndId(pixTransfer.getEndToEndId())
                .fromWalletId(pixTransfer.getFromWalletId())
                .toWalletId(pixTransfer.getToWalletId())
                .amount(pixTransfer.getAmount().getAmount())
                .status(pixTransfer.getStatus())
                .createdAt(pixTransfer.getCreatedAt())
                .confirmedAt(pixTransfer.getConfirmedAt())
                .rejectedAt(pixTransfer.getRejectedAt())
                .build();
    }

    private PixTransfer toDomain(PixTransferJpaEntity entity) {
        return PixTransfer.builder()
                .endToEndId(entity.getEndToEndId())
                .fromWalletId(entity.getFromWalletId())
                .toWalletId(entity.getToWalletId())
                .amount(Money.of(entity.getAmount()))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .confirmedAt(entity.getConfirmedAt())
                .rejectedAt(entity.getRejectedAt())
                .build();
    }
}

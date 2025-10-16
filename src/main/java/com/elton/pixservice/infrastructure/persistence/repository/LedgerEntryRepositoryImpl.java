package com.elton.pixservice.infrastructure.persistence.repository;

import com.elton.pixservice.domain.entity.LedgerEntry;
import com.elton.pixservice.domain.repository.LedgerEntryRepository;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LedgerEntryRepositoryImpl implements LedgerEntryRepository {

    private final LedgerEntryJpaRepository jpaRepository;

    @Override
    @Transactional
    public LedgerEntry save(LedgerEntry ledgerEntry) {
        LedgerEntryJpaEntity entity = toJpaEntity(ledgerEntry);
        LedgerEntryJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> findByWalletId(Long walletId) {
        return jpaRepository.findByWalletId(walletId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> findByWalletIdAndCreatedAtBefore(Long walletId, LocalDateTime timestamp) {
        return jpaRepository.findByWalletIdAndCreatedAtBefore(walletId, timestamp).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private LedgerEntryJpaEntity toJpaEntity(LedgerEntry ledgerEntry) {
        return LedgerEntryJpaEntity.builder()
                .id(ledgerEntry.getId())
                .walletId(ledgerEntry.getWalletId())
                .amount(ledgerEntry.getAmount().getAmount())
                .type(ledgerEntry.getType())
                .endToEndId(ledgerEntry.getEndToEndId())
                .metadata(ledgerEntry.getMetadata())
                .createdAt(ledgerEntry.getCreatedAt())
                .build();
    }

    private LedgerEntry toDomain(LedgerEntryJpaEntity entity) {
        return LedgerEntry.builder()
                .id(entity.getId())
                .walletId(entity.getWalletId())
                .amount(Money.of(entity.getAmount()))
                .type(entity.getType())
                .endToEndId(entity.getEndToEndId())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

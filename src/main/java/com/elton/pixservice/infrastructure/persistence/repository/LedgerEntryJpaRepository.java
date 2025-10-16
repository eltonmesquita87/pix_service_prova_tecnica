package com.elton.pixservice.infrastructure.persistence.repository;

import com.elton.pixservice.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, Long> {
    List<LedgerEntryJpaEntity> findByWalletId(Long walletId);
    List<LedgerEntryJpaEntity> findByWalletIdAndCreatedAtBefore(Long walletId, LocalDateTime timestamp);
}

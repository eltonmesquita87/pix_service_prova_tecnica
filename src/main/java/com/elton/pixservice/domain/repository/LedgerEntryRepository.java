package com.elton.pixservice.domain.repository;

import com.elton.pixservice.domain.entity.LedgerEntry;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for LedgerEntry entity (Port).
 */
public interface LedgerEntryRepository {
    LedgerEntry save(LedgerEntry ledgerEntry);
    List<LedgerEntry> findByWalletIdAndCreatedAtBefore(Long walletId, LocalDateTime timestamp);
}

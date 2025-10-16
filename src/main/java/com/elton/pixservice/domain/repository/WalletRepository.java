package com.elton.pixservice.domain.repository;

import com.elton.pixservice.domain.entity.Wallet;

import java.util.Optional;

/**
 * Repository interface for Wallet entity (Port).
 * Implementation will be in infrastructure layer.
 */
public interface WalletRepository {
    Wallet save(Wallet wallet);
    Optional<Wallet> findById(Long id);
    Optional<Wallet> findByIdWithLock(Long id);
    boolean existsById(Long id);
}

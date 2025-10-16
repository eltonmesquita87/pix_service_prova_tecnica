package com.elton.pixservice.domain.repository;

import com.elton.pixservice.domain.entity.PixKey;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PixKey entity (Port).
 */
public interface PixKeyRepository {
    PixKey save(PixKey pixKey);
    Optional<PixKey> findByKeyValue(String keyValue);
    List<PixKey> findByWalletId(Long walletId);
    boolean existsByKeyValue(String keyValue);
}

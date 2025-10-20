package com.elton.pixservice.domain.repository;

import com.elton.pixservice.domain.entity.PixTransfer;

import java.util.Optional;

/**
 * Repository interface for PixTransfer entity (Port).
 */
public interface PixTransferRepository {
    PixTransfer save(PixTransfer pixTransfer);
    Optional<PixTransfer> findByEndToEndIdWithLock(String endToEndId);
}

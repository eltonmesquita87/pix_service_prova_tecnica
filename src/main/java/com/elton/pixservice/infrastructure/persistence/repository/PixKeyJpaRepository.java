package com.elton.pixservice.infrastructure.persistence.repository;

import com.elton.pixservice.infrastructure.persistence.entity.PixKeyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PixKeyJpaRepository extends JpaRepository<PixKeyJpaEntity, Long> {
    Optional<PixKeyJpaEntity> findByKeyValue(String keyValue);
    List<PixKeyJpaEntity> findByWalletId(Long walletId);
    boolean existsByKeyValue(String keyValue);
}

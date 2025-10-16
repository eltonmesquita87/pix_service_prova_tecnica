package com.elton.pixservice.infrastructure.persistence.repository;

import com.elton.pixservice.infrastructure.persistence.entity.PixTransferJpaEntity;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PixTransferJpaRepository extends JpaRepository<PixTransferJpaEntity, String> {
    Optional<PixTransferJpaEntity> findByEndToEndId(String endToEndId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PixTransferJpaEntity p WHERE p.endToEndId = :endToEndId")
    Optional<PixTransferJpaEntity> findByEndToEndIdWithLock(@Param("endToEndId") String endToEndId);
}

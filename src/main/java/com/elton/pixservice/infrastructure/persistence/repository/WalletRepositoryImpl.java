package com.elton.pixservice.infrastructure.persistence.repository;

import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.repository.WalletRepository;
import com.elton.pixservice.domain.valueobject.Cpf;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.domain.valueobject.WalletHolder;
import com.elton.pixservice.infrastructure.persistence.entity.WalletJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WalletRepositoryImpl implements WalletRepository {

    private final WalletJpaRepository jpaRepository;

    @Override
    @Transactional
    public Wallet save(Wallet wallet) {
        WalletJpaEntity entity = toJpaEntity(wallet);
        WalletJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Wallet> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public Optional<Wallet> findByIdWithLock(Long id) {
        return jpaRepository.findByIdWithLock(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCpf(Cpf cpf) {
        return jpaRepository.existsByCpf(cpf.getDigits());
    }

    private WalletJpaEntity toJpaEntity(Wallet wallet) {
        WalletHolder holder = wallet.getHolder() == null ? WalletHolder.empty() : wallet.getHolder();
        return WalletJpaEntity.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance().getAmount())
                .version(wallet.getVersion())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .cpf(holder.getCpf() == null ? null : holder.getCpf().getDigits())
                .nome(holder.getNome())
                .email(holder.getEmail())
                .telefone(holder.getTelefone())
                .build();
    }

    private Wallet toDomain(WalletJpaEntity entity) {
        return Wallet.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .balance(Money.of(entity.getBalance()))
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .holder(new WalletHolder(
                        entity.getCpf() == null ? null : Cpf.restore(entity.getCpf()),
                        entity.getNome(),
                        entity.getEmail(),
                        entity.getTelefone()))
                .build();
    }
}

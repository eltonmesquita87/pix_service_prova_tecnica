package com.elton.pixservice.usecase;

import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.exception.CpfJaCadastradoException;
import com.elton.pixservice.domain.repository.WalletRepository;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.domain.valueobject.WalletHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateWalletUseCase {

    private final WalletRepository walletRepository;

    @Transactional
    public Wallet execute(String userId) {
        return execute(userId, WalletHolder.empty());
    }

    @Transactional
    public Wallet execute(String userId, WalletHolder holder) {
        log.info("Creating wallet for user: {}", userId);

        if (holder.getCpf() != null && walletRepository.existsByCpf(holder.getCpf())) {
            throw new CpfJaCadastradoException();
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(Money.of(BigDecimal.ZERO))
                .version(0L)
                .holder(holder)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet created successfully with id: {} for user: {}", savedWallet.getId(), userId);

        return savedWallet;
    }
}

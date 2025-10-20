package com.elton.pixservice.usecase;

import com.elton.pixservice.domain.entity.LedgerEntry;
import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.exception.WalletNaoEncontradaException;
import com.elton.pixservice.domain.repository.LedgerEntryRepository;
import com.elton.pixservice.domain.repository.WalletRepository;
import com.elton.pixservice.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetBalanceUseCase {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(readOnly = true)
    public Money getCurrentBalance(Long walletId) {
        log.info("Getting current balance for wallet: {}", walletId);

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNaoEncontradaException(walletId));

        return wallet.getBalance();
    }

    @Transactional(readOnly = true)
    public Money getHistoricalBalance(Long walletId, LocalDateTime timestamp) {
        log.info("Getting historical balance for wallet: {} at timestamp: {}", walletId, timestamp);

        // Verify wallet exists
        if (!walletRepository.existsById(walletId)) {
            throw new WalletNaoEncontradaException(walletId);
        }

        // Calculate balance from ledger entries up to the timestamp
        List<LedgerEntry> entries = ledgerEntryRepository.findByWalletIdAndCreatedAtBefore(walletId, timestamp);

        BigDecimal balance = entries.stream()
                .map(LedgerEntry::getSignedAmount)
                .map(Money::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Historical balance calculated for wallet: {} = {}", walletId, balance);

        return Money.of(balance);
    }
}

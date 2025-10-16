package com.elton.pixservice.usecase;

import com.elton.pixservice.domain.entity.LedgerEntry;
import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.repository.LedgerEntryRepository;
import com.elton.pixservice.domain.repository.WalletRepository;
import com.elton.pixservice.domain.valueobject.LedgerEntryType;
import com.elton.pixservice.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepositUseCase {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public Wallet execute(Long walletId, Money amount) {
        log.info("Processing deposit for wallet: {}, amount: {}", walletId, amount);

        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));

        wallet.deposit(amount);
        Wallet updatedWallet = walletRepository.save(wallet);

        // Create ledger entry
        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .walletId(walletId)
                .amount(amount)
                .type(LedgerEntryType.DEPOSIT)
                .metadata("Deposit operation")
                .build();
        ledgerEntryRepository.save(ledgerEntry);

        log.info("Deposit processed successfully for wallet: {}, new balance: {}", walletId, updatedWallet.getBalance());

        return updatedWallet;
    }
}

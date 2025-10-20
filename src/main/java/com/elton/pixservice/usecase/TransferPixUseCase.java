package com.elton.pixservice.usecase;

import com.elton.pixservice.domain.entity.LedgerEntry;
import com.elton.pixservice.domain.entity.PixKey;
import com.elton.pixservice.domain.entity.PixTransfer;
import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.exception.RequisicaoDuplicadaException;
import com.elton.pixservice.domain.exception.SaldoInsuficienteException;
import com.elton.pixservice.domain.exception.TransferenciaInvalidaException;
import com.elton.pixservice.domain.exception.WalletNaoEncontradaException;
import com.elton.pixservice.domain.repository.*;
import com.elton.pixservice.domain.valueobject.LedgerEntryType;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.domain.valueobject.TransferStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferPixUseCase {

    private final WalletRepository walletRepository;
    private final PixKeyRepository pixKeyRepository;
    private final PixTransferRepository pixTransferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyRepository idempotencyRepository;

    @Transactional
    public PixTransfer execute(Long fromWalletId, String pixKey, Money amount, String idempotencyKey) {
        log.info("Processing Pix transfer - from: {}, to: {}, amount: {}, idempotencyKey: {}",
                fromWalletId, pixKey, amount, idempotencyKey);

        // Check idempotency
        String scope = "pix_transfer";
        if (idempotencyRepository.exists(scope, idempotencyKey)) {
            log.info("Duplicate request detected for idempotencyKey: {}", idempotencyKey);
            // In a real implementation, return stored response
            throw new RequisicaoDuplicadaException();
        }

        // Resolve Pix key to destination wallet
        PixKey destinationPixKey = pixKeyRepository.findByKeyValue(pixKey)
                .orElseThrow(() -> new WalletNaoEncontradaException("Pix key not found: " + pixKey));

        Long toWalletId = destinationPixKey.getWalletId();

        if (fromWalletId.equals(toWalletId)) {
            throw TransferenciaInvalidaException.mesmaCarteira();
        }

        // Lock and validate source wallet
        Wallet sourceWallet = walletRepository.findByIdWithLock(fromWalletId)
                .orElseThrow(() -> new WalletNaoEncontradaException("Source wallet not found: " + fromWalletId));

        if (!sourceWallet.hasSufficientBalance(amount)) {
            throw new SaldoInsuficienteException();
        }

        // Validate destination wallet exists
        if (!walletRepository.existsById(toWalletId)) {
            throw new WalletNaoEncontradaException("Destination wallet not found: " + toWalletId);
        }

        // Generate unique endToEndId
        String endToEndId = "E" + UUID.randomUUID().toString().replace("-", "");

        // Debit source wallet
        sourceWallet.withdraw(amount);
        walletRepository.save(sourceWallet);

        // Create transfer record with PENDING status
        PixTransfer transfer = PixTransfer.builder()
                .endToEndId(endToEndId)
                .fromWalletId(fromWalletId)
                .toWalletId(toWalletId)
                .amount(amount)
                .status(TransferStatus.PENDING)
                .build();
        pixTransferRepository.save(transfer);

        // Create ledger entries
        LedgerEntry debitEntry = LedgerEntry.builder()
                .walletId(fromWalletId)
                .amount(amount)
                .type(LedgerEntryType.TRANSFER_DEBIT)
                .endToEndId(endToEndId)
                .metadata("Pix transfer to " + pixKey)
                .build();
        ledgerEntryRepository.save(debitEntry);

        // Save idempotency key
        idempotencyRepository.saveIdempotencyKey(scope, idempotencyKey, endToEndId);

        log.info("Pix transfer created successfully with endToEndId: {}", endToEndId);

        return transfer;
    }
}

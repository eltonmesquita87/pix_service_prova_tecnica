package com.elton.pixservice.usecase;

import com.elton.pixservice.domain.entity.LedgerEntry;
import com.elton.pixservice.domain.entity.PixTransfer;
import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.exception.TipoEventoDesconhecidoException;
import com.elton.pixservice.domain.exception.WalletNaoEncontradaException;
import com.elton.pixservice.domain.repository.*;
import com.elton.pixservice.domain.valueobject.LedgerEntryType;
import com.elton.pixservice.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessWebhookUseCase {

    private final PixTransferRepository pixTransferRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final WebhookEventRepository webhookEventRepository;

    @Transactional
    public void execute(String eventId, String endToEndId, String eventType) {
        log.info("Processing webhook - eventId: {}, endToEndId: {}, type: {}", eventId, endToEndId, eventType);

        // Check if event already processed (idempotency)
        if (webhookEventRepository.eventAlreadyProcessed(eventId)) {
            log.info("Event already processed, skipping: {}", eventId);
            return;
        }

        // Find transfer
        PixTransfer transfer = pixTransferRepository.findByEndToEndIdWithLock(endToEndId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + endToEndId));

        switch (eventType) {
            case "CONFIRMED":
                processConfirmation(transfer);
                break;
            case "REJECTED":
                processRejection(transfer);
                break;
            default:
                throw new TipoEventoDesconhecidoException(eventType);
        }

        // Mark event as processed
        webhookEventRepository.saveEvent(eventId, endToEndId, eventType);

        log.info("Webhook processed successfully - eventId: {}", eventId);
    }

    private void processConfirmation(PixTransfer transfer) {
        if (!transfer.getStatus().canTransitionTo(com.elton.pixservice.domain.valueobject.TransferStatus.CONFIRMED)) {
            log.warn("Invalid state transition for transfer: {}, current status: {}",
                    transfer.getEndToEndId(), transfer.getStatus());
            return;
        }

        transfer.confirm();
        pixTransferRepository.save(transfer);

        // Credit destination wallet
        Wallet destinationWallet = walletRepository.findByIdWithLock(transfer.getToWalletId())
                .orElseThrow(() -> new WalletNaoEncontradaException(transfer.getToWalletId()));

        destinationWallet.deposit(transfer.getAmount());
        walletRepository.save(destinationWallet);

        // Create credit ledger entry
        LedgerEntry creditEntry = LedgerEntry.builder()
                .walletId(transfer.getToWalletId())
                .amount(transfer.getAmount())
                .type(LedgerEntryType.TRANSFER_CREDIT)
                .endToEndId(transfer.getEndToEndId())
                .metadata("Pix transfer confirmed from wallet " + transfer.getFromWalletId())
                .build();
        ledgerEntryRepository.save(creditEntry);

        log.info("Transfer confirmed: {}", transfer.getEndToEndId());
    }

    private void processRejection(PixTransfer transfer) {
        if (!transfer.getStatus().canTransitionTo(com.elton.pixservice.domain.valueobject.TransferStatus.REJECTED)) {
            log.warn("Invalid state transition for transfer: {}, current status: {}",
                    transfer.getEndToEndId(), transfer.getStatus());
            return;
        }

        transfer.reject();
        pixTransferRepository.save(transfer);

        // Refund source wallet
        Wallet sourceWallet = walletRepository.findByIdWithLock(transfer.getFromWalletId())
                .orElseThrow(() -> new WalletNaoEncontradaException(transfer.getFromWalletId()));

        sourceWallet.deposit(transfer.getAmount());
        walletRepository.save(sourceWallet);

        // Create refund ledger entry
        LedgerEntry refundEntry = LedgerEntry.builder()
                .walletId(transfer.getFromWalletId())
                .amount(transfer.getAmount())
                .type(LedgerEntryType.DEPOSIT)
                .endToEndId(transfer.getEndToEndId())
                .metadata("Pix transfer rejected - refund")
                .build();
        ledgerEntryRepository.save(refundEntry);

        log.info("Transfer rejected and refunded: {}", transfer.getEndToEndId());
    }
}

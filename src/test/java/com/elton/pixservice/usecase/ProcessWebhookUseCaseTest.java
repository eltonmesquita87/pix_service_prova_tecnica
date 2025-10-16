package com.elton.pixservice.usecase;

import com.elton.pixservice.domain.entity.PixTransfer;
import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.repository.*;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.domain.valueobject.TransferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessWebhookUseCase Tests")
class ProcessWebhookUseCaseTest {

    @Mock
    private PixTransferRepository pixTransferRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @InjectMocks
    private ProcessWebhookUseCase processWebhookUseCase;

    private PixTransfer pendingTransfer;
    private Wallet destinationWallet;
    private Wallet sourceWallet;

    @BeforeEach
    void setUp() {
        pendingTransfer = PixTransfer.builder()
                .endToEndId("E123456")
                .fromWalletId(1L)
                .toWalletId(2L)
                .amount(Money.of(150.00))
                .status(TransferStatus.PENDING)
                .build();

        destinationWallet = Wallet.builder()
                .id(2L)
                .userId("user2")
                .balance(Money.of(100.00))
                .version(0L)
                .build();

        sourceWallet = Wallet.builder()
                .id(1L)
                .userId("user1")
                .balance(Money.of(350.00))
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("Should process CONFIRMED webhook successfully")
    void shouldProcessConfirmedWebhookSuccessfully() {
        // Given
        String eventId = "evt_123";
        String endToEndId = "E123456";
        String eventType = "CONFIRMED";

        when(webhookEventRepository.eventAlreadyProcessed(eventId)).thenReturn(false);
        when(pixTransferRepository.findByEndToEndIdWithLock(endToEndId))
                .thenReturn(Optional.of(pendingTransfer));
        when(walletRepository.findByIdWithLock(2L)).thenReturn(Optional.of(destinationWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(pixTransferRepository.save(any(PixTransfer.class))).thenAnswer(i -> i.getArgument(0));
        when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        processWebhookUseCase.execute(eventId, endToEndId, eventType);

        // Then
        verify(webhookEventRepository).eventAlreadyProcessed(eventId);
        verify(pixTransferRepository).findByEndToEndIdWithLock(endToEndId);
        verify(pixTransferRepository).save(argThat(transfer ->
            transfer.getStatus() == TransferStatus.CONFIRMED &&
            transfer.getConfirmedAt() != null
        ));
        verify(walletRepository).findByIdWithLock(2L);
        verify(walletRepository).save(argThat(wallet ->
            wallet.getId().equals(2L) &&
            wallet.getBalance().equals(Money.of(250.00)) // 100 + 150
        ));
        verify(ledgerEntryRepository).save(any());
        verify(webhookEventRepository).saveEvent(eventId, endToEndId, eventType);
    }

    @Test
    @DisplayName("Should process REJECTED webhook successfully")
    void shouldProcessRejectedWebhookSuccessfully() {
        // Given
        String eventId = "evt_456";
        String endToEndId = "E123456";
        String eventType = "REJECTED";

        when(webhookEventRepository.eventAlreadyProcessed(eventId)).thenReturn(false);
        when(pixTransferRepository.findByEndToEndIdWithLock(endToEndId))
                .thenReturn(Optional.of(pendingTransfer));
        when(walletRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(pixTransferRepository.save(any(PixTransfer.class))).thenAnswer(i -> i.getArgument(0));
        when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        processWebhookUseCase.execute(eventId, endToEndId, eventType);

        // Then
        verify(pixTransferRepository).save(argThat(transfer ->
            transfer.getStatus() == TransferStatus.REJECTED &&
            transfer.getRejectedAt() != null
        ));
        verify(walletRepository).findByIdWithLock(1L);
        verify(walletRepository).save(argThat(wallet ->
            wallet.getId().equals(1L) &&
            wallet.getBalance().equals(Money.of(500.00)) // 350 + 150 (refund)
        ));
        verify(ledgerEntryRepository).save(any());
        verify(webhookEventRepository).saveEvent(eventId, endToEndId, eventType);
    }

    @Test
    @DisplayName("Should skip processing when event already processed (idempotency)")
    void shouldSkipProcessingWhenEventAlreadyProcessed() {
        // Given
        String eventId = "evt_duplicate";
        String endToEndId = "E123456";
        String eventType = "CONFIRMED";

        when(webhookEventRepository.eventAlreadyProcessed(eventId)).thenReturn(true);

        // When
        processWebhookUseCase.execute(eventId, endToEndId, eventType);

        // Then
        verify(webhookEventRepository).eventAlreadyProcessed(eventId);
        verify(pixTransferRepository, never()).findByEndToEndIdWithLock(anyString());
        verify(walletRepository, never()).findByIdWithLock(any());
        verify(webhookEventRepository, never()).saveEvent(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when transfer not found")
    void shouldThrowExceptionWhenTransferNotFound() {
        // Given
        String eventId = "evt_789";
        String endToEndId = "E_NOT_FOUND";
        String eventType = "CONFIRMED";

        when(webhookEventRepository.eventAlreadyProcessed(eventId)).thenReturn(false);
        when(pixTransferRepository.findByEndToEndIdWithLock(endToEndId))
                .thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> processWebhookUseCase.execute(eventId, endToEndId, eventType)
        );
        assertTrue(exception.getMessage().contains("Transfer not found"));

        verify(webhookEventRepository, never()).saveEvent(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception for unknown event type")
    void shouldThrowExceptionForUnknownEventType() {
        // Given
        String eventId = "evt_unknown";
        String endToEndId = "E123456";
        String eventType = "UNKNOWN";

        when(webhookEventRepository.eventAlreadyProcessed(eventId)).thenReturn(false);
        when(pixTransferRepository.findByEndToEndIdWithLock(endToEndId))
                .thenReturn(Optional.of(pendingTransfer));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> processWebhookUseCase.execute(eventId, endToEndId, eventType)
        );
        assertTrue(exception.getMessage().contains("Unknown event type"));
    }

    @Test
    @DisplayName("Should not process invalid state transition")
    void shouldNotProcessInvalidStateTransition() {
        // Given
        String eventId = "evt_invalid";
        String endToEndId = "E123456";
        String eventType = "CONFIRMED";

        PixTransfer alreadyConfirmedTransfer = PixTransfer.builder()
                .endToEndId(endToEndId)
                .status(TransferStatus.CONFIRMED) // Already confirmed
                .build();

        when(webhookEventRepository.eventAlreadyProcessed(eventId)).thenReturn(false);
        when(pixTransferRepository.findByEndToEndIdWithLock(endToEndId))
                .thenReturn(Optional.of(alreadyConfirmedTransfer));

        // When
        processWebhookUseCase.execute(eventId, endToEndId, eventType);

        // Then
        verify(walletRepository, never()).findByIdWithLock(any());
        verify(walletRepository, never()).save(any());
        verify(webhookEventRepository).saveEvent(eventId, endToEndId, eventType);
    }

    @Test
    @DisplayName("Should use pessimistic locking")
    void shouldUsePessimisticLocking() {
        // Given
        String eventId = "evt_lock";
        String endToEndId = "E123456";
        String eventType = "CONFIRMED";

        when(webhookEventRepository.eventAlreadyProcessed(eventId)).thenReturn(false);
        when(pixTransferRepository.findByEndToEndIdWithLock(endToEndId))
                .thenReturn(Optional.of(pendingTransfer));
        when(walletRepository.findByIdWithLock(2L)).thenReturn(Optional.of(destinationWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(pixTransferRepository.save(any(PixTransfer.class))).thenAnswer(i -> i.getArgument(0));
        when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        processWebhookUseCase.execute(eventId, endToEndId, eventType);

        // Then
        verify(pixTransferRepository).findByEndToEndIdWithLock(endToEndId);
        verify(pixTransferRepository, never()).findByEndToEndId(anyString());
        verify(walletRepository).findByIdWithLock(2L);
        verify(walletRepository, never()).findById(any());
    }
}

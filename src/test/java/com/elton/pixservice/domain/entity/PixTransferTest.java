package com.elton.pixservice.domain.entity;

import com.elton.pixservice.domain.exception.TransferenciaInvalidaException;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.domain.valueobject.TransferStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PixTransfer Entity Tests")
class PixTransferTest {

    @Test
    @DisplayName("Should create transfer in PENDING status")
    void shouldCreateTransferInPendingStatus() {
        // Given & When
        PixTransfer transfer = PixTransfer.builder()
                .endToEndId("E123")
                .fromWalletId(1L)
                .toWalletId(2L)
                .amount(Money.of(100.00))
                .status(TransferStatus.PENDING)
                .build();

        // Then
        assertNotNull(transfer);
        assertEquals(TransferStatus.PENDING, transfer.getStatus());
        assertTrue(transfer.isPending());
        assertFalse(transfer.isConfirmed());
        assertFalse(transfer.isRejected());
    }

    @Test
    @DisplayName("Should confirm transfer successfully")
    void shouldConfirmTransferSuccessfully() {
        // Given
        PixTransfer transfer = PixTransfer.builder()
                .endToEndId("E123")
                .status(TransferStatus.PENDING)
                .build();

        // When
        transfer.confirm();

        // Then
        assertEquals(TransferStatus.CONFIRMED, transfer.getStatus());
        assertTrue(transfer.isConfirmed());
        assertNotNull(transfer.getConfirmedAt());
        assertNull(transfer.getRejectedAt());
    }

    @Test
    @DisplayName("Should reject transfer successfully")
    void shouldRejectTransferSuccessfully() {
        // Given
        PixTransfer transfer = PixTransfer.builder()
                .endToEndId("E123")
                .status(TransferStatus.PENDING)
                .build();

        // When
        transfer.reject();

        // Then
        assertEquals(TransferStatus.REJECTED, transfer.getStatus());
        assertTrue(transfer.isRejected());
        assertNotNull(transfer.getRejectedAt());
        assertNull(transfer.getConfirmedAt());
    }

    @Test
    @DisplayName("Should throw exception when confirming already confirmed transfer")
    void shouldThrowExceptionWhenConfirmingAlreadyConfirmedTransfer() {
        // Given
        PixTransfer transfer = PixTransfer.builder()
                .status(TransferStatus.CONFIRMED)
                .build();

        // When & Then
        TransferenciaInvalidaException exception = assertThrows(
            TransferenciaInvalidaException.class,
            transfer::confirm
        );
        assertTrue(exception.getMessage().contains("Cannot transition from CONFIRMED to CONFIRMED"));
    }

    @Test
    @DisplayName("Should throw exception when rejecting already confirmed transfer")
    void shouldThrowExceptionWhenRejectingAlreadyConfirmedTransfer() {
        // Given
        PixTransfer transfer = PixTransfer.builder()
                .status(TransferStatus.CONFIRMED)
                .build();

        // When & Then
        TransferenciaInvalidaException exception = assertThrows(
            TransferenciaInvalidaException.class,
            transfer::reject
        );
        assertTrue(exception.getMessage().contains("Cannot transition from CONFIRMED to REJECTED"));
    }

    @Test
    @DisplayName("Should throw exception when confirming already rejected transfer")
    void shouldThrowExceptionWhenConfirmingAlreadyRejectedTransfer() {
        // Given
        PixTransfer transfer = PixTransfer.builder()
                .status(TransferStatus.REJECTED)
                .build();

        // When & Then
        TransferenciaInvalidaException exception = assertThrows(
            TransferenciaInvalidaException.class,
            transfer::confirm
        );
        assertTrue(exception.getMessage().contains("Cannot transition from REJECTED to CONFIRMED"));
    }

    @Test
    @DisplayName("Should throw exception when rejecting already rejected transfer")
    void shouldThrowExceptionWhenRejectingAlreadyRejectedTransfer() {
        // Given
        PixTransfer transfer = PixTransfer.builder()
                .status(TransferStatus.REJECTED)
                .build();

        // When & Then
        TransferenciaInvalidaException exception = assertThrows(
            TransferenciaInvalidaException.class,
            transfer::reject
        );
        assertTrue(exception.getMessage().contains("Cannot transition from REJECTED to REJECTED"));
    }

    @Test
    @DisplayName("Should check transfer status correctly")
    void shouldCheckTransferStatusCorrectly() {
        // Given
        PixTransfer pending = PixTransfer.builder().status(TransferStatus.PENDING).build();
        PixTransfer confirmed = PixTransfer.builder().status(TransferStatus.CONFIRMED).build();
        PixTransfer rejected = PixTransfer.builder().status(TransferStatus.REJECTED).build();

        // When & Then
        assertTrue(pending.isPending());
        assertFalse(pending.isConfirmed());
        assertFalse(pending.isRejected());

        assertFalse(confirmed.isPending());
        assertTrue(confirmed.isConfirmed());
        assertFalse(confirmed.isRejected());

        assertFalse(rejected.isPending());
        assertFalse(rejected.isConfirmed());
        assertTrue(rejected.isRejected());
    }

    @Test
    @DisplayName("Should build transfer with all properties")
    void shouldBuildTransferWithAllProperties() {
        // When
        PixTransfer transfer = PixTransfer.builder()
                .endToEndId("E123456")
                .fromWalletId(1L)
                .toWalletId(2L)
                .amount(Money.of(150.00))
                .status(TransferStatus.PENDING)
                .build();

        // Then
        assertNotNull(transfer);
        assertEquals("E123456", transfer.getEndToEndId());
        assertEquals(1L, transfer.getFromWalletId());
        assertEquals(2L, transfer.getToWalletId());
        assertEquals(Money.of(150.00), transfer.getAmount());
        assertEquals(TransferStatus.PENDING, transfer.getStatus());
    }
}

package com.elton.pixservice.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransferStatus Value Object Tests")
class TransferStatusTest {

    @Test
    @DisplayName("Should have three status values")
    void shouldHaveThreeStatusValues() {
        // When
        TransferStatus[] values = TransferStatus.values();

        // Then
        assertEquals(3, values.length);
        assertArrayEquals(
            new TransferStatus[]{TransferStatus.PENDING, TransferStatus.CONFIRMED, TransferStatus.REJECTED},
            values
        );
    }

    @ParameterizedTest
    @CsvSource({
        "PENDING, CONFIRMED, true",
        "PENDING, REJECTED, true",
        "CONFIRMED, REJECTED, false",
        "CONFIRMED, PENDING, false",
        "REJECTED, CONFIRMED, false",
        "REJECTED, PENDING, false"
    })
    @DisplayName("Should validate state transitions")
    void shouldValidateStateTransitions(TransferStatus from, TransferStatus to, boolean canTransition) {
        // When & Then
        assertEquals(canTransition, from.canTransitionTo(to));
    }

    @Test
    @DisplayName("PENDING can transition to CONFIRMED")
    void pendingCanTransitionToConfirmed() {
        // Given
        TransferStatus pending = TransferStatus.PENDING;

        // When & Then
        assertTrue(pending.canTransitionTo(TransferStatus.CONFIRMED));
    }

    @Test
    @DisplayName("PENDING can transition to REJECTED")
    void pendingCanTransitionToRejected() {
        // Given
        TransferStatus pending = TransferStatus.PENDING;

        // When & Then
        assertTrue(pending.canTransitionTo(TransferStatus.REJECTED));
    }

    @Test
    @DisplayName("PENDING cannot transition to itself")
    void pendingCannotTransitionToItself() {
        // Given
        TransferStatus pending = TransferStatus.PENDING;

        // When & Then
        assertFalse(pending.canTransitionTo(TransferStatus.PENDING));
    }

    @Test
    @DisplayName("CONFIRMED is terminal state")
    void confirmedIsTerminalState() {
        // Given
        TransferStatus confirmed = TransferStatus.CONFIRMED;

        // When & Then
        assertFalse(confirmed.canTransitionTo(TransferStatus.PENDING));
        assertFalse(confirmed.canTransitionTo(TransferStatus.CONFIRMED));
        assertFalse(confirmed.canTransitionTo(TransferStatus.REJECTED));
    }

    @Test
    @DisplayName("REJECTED is terminal state")
    void rejectedIsTerminalState() {
        // Given
        TransferStatus rejected = TransferStatus.REJECTED;

        // When & Then
        assertFalse(rejected.canTransitionTo(TransferStatus.PENDING));
        assertFalse(rejected.canTransitionTo(TransferStatus.CONFIRMED));
        assertFalse(rejected.canTransitionTo(TransferStatus.REJECTED));
    }

    @Test
    @DisplayName("Should convert from string")
    void shouldConvertFromString() {
        // When & Then
        assertEquals(TransferStatus.PENDING, TransferStatus.valueOf("PENDING"));
        assertEquals(TransferStatus.CONFIRMED, TransferStatus.valueOf("CONFIRMED"));
        assertEquals(TransferStatus.REJECTED, TransferStatus.valueOf("REJECTED"));
    }

    @Test
    @DisplayName("Should throw exception for invalid string")
    void shouldThrowExceptionForInvalidString() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> TransferStatus.valueOf("INVALID"));
    }
}

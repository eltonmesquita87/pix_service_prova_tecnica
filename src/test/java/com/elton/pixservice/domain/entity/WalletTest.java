package com.elton.pixservice.domain.entity;

import com.elton.pixservice.domain.exception.SaldoInsuficienteException;
import com.elton.pixservice.domain.exception.ValorInvalidoException;
import com.elton.pixservice.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Wallet Entity Tests")
class WalletTest {

    @Test
    @DisplayName("Should deposit money successfully")
    void shouldDepositMoneySuccessfully() {
        // Given
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId("user123")
                .balance(Money.of(100.00))
                .version(0L)
                .build();

        // When
        wallet.deposit(Money.of(50.00));

        // Then
        assertEquals(Money.of(150.00), wallet.getBalance());
    }

    @Test
    @DisplayName("Should throw exception when depositing zero")
    void shouldThrowExceptionWhenDepositingZero() {
        // Given
        Wallet wallet = Wallet.builder()
                .balance(Money.of(100.00))
                .build();

        // When & Then
        assertThrows(ValorInvalidoException.class, () -> wallet.deposit(Money.zero()));
    }

    @Test
    @DisplayName("Should throw exception when depositing negative amount")
    void shouldThrowExceptionWhenDepositingNegativeAmount() {
        // Given
        Wallet wallet = Wallet.builder()
                .balance(Money.of(100.00))
                .build();

        // When & Then
        assertThrows(ValorInvalidoException.class, () -> wallet.deposit(Money.of(-10.00)));
    }

    @Test
    @DisplayName("Should withdraw money successfully")
    void shouldWithdrawMoneySuccessfully() {
        // Given
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId("user123")
                .balance(Money.of(100.00))
                .version(0L)
                .build();

        // When
        wallet.withdraw(Money.of(30.00));

        // Then
        assertEquals(Money.of(70.00), wallet.getBalance());
    }

    @Test
    @DisplayName("Should throw exception when withdrawing more than balance")
    void shouldThrowExceptionWhenWithdrawingMoreThanBalance() {
        // Given
        Wallet wallet = Wallet.builder()
                .balance(Money.of(100.00))
                .build();

        // When & Then
        SaldoInsuficienteException exception = assertThrows(
            SaldoInsuficienteException.class,
            () -> wallet.withdraw(Money.of(150.00))
        );
        assertTrue(exception.getMessage().contains("Insufficient balance"));
    }

    @Test
    @DisplayName("Should throw exception when withdrawing zero")
    void shouldThrowExceptionWhenWithdrawingZero() {
        // Given
        Wallet wallet = Wallet.builder()
                .balance(Money.of(100.00))
                .build();

        // When & Then
        assertThrows(ValorInvalidoException.class, () -> wallet.withdraw(Money.zero()));
    }

    @Test
    @DisplayName("Should throw exception when withdrawing negative amount")
    void shouldThrowExceptionWhenWithdrawingNegativeAmount() {
        // Given
        Wallet wallet = Wallet.builder()
                .balance(Money.of(100.00))
                .build();

        // When & Then
        assertThrows(ValorInvalidoException.class, () -> wallet.withdraw(Money.of(-10.00)));
    }

    @Test
    @DisplayName("Should check if has sufficient balance")
    void shouldCheckIfHasSufficientBalance() {
        // Given
        Wallet wallet = Wallet.builder()
                .balance(Money.of(100.00))
                .build();

        // When & Then
        assertTrue(wallet.hasSufficientBalance(Money.of(100.00)));
        assertTrue(wallet.hasSufficientBalance(Money.of(50.00)));
        assertFalse(wallet.hasSufficientBalance(Money.of(150.00)));
    }

    @Test
    @DisplayName("Should allow withdrawing exact balance")
    void shouldAllowWithdrawingExactBalance() {
        // Given
        Wallet wallet = Wallet.builder()
                .balance(Money.of(100.00))
                .build();

        // When
        wallet.withdraw(Money.of(100.00));

        // Then
        assertEquals(Money.zero(), wallet.getBalance());
    }

    @Test
    @DisplayName("Should handle multiple operations")
    void shouldHandleMultipleOperations() {
        // Given
        Wallet wallet = Wallet.builder()
                .balance(Money.zero())
                .build();

        // When
        wallet.deposit(Money.of(100.00));
        wallet.withdraw(Money.of(30.00));
        wallet.deposit(Money.of(50.00));
        wallet.withdraw(Money.of(20.00));

        // Then
        assertEquals(Money.of(100.00), wallet.getBalance());
    }

    @Test
    @DisplayName("Should create wallet with builder")
    void shouldCreateWalletWithBuilder() {
        // When
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId("user123")
                .balance(Money.of(100.00))
                .version(0L)
                .build();

        // Then
        assertNotNull(wallet);
        assertEquals(1L, wallet.getId());
        assertEquals("user123", wallet.getUserId());
        assertEquals(Money.of(100.00), wallet.getBalance());
        assertEquals(0L, wallet.getVersion());
    }
}

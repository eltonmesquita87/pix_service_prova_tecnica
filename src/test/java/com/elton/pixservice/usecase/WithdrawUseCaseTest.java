package com.elton.pixservice.usecase;

import com.elton.pixservice.domain.entity.LedgerEntry;
import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.exception.SaldoInsuficienteException;
import com.elton.pixservice.domain.exception.WalletNaoEncontradaException;
import com.elton.pixservice.domain.repository.LedgerEntryRepository;
import com.elton.pixservice.domain.repository.WalletRepository;
import com.elton.pixservice.domain.valueobject.LedgerEntryType;
import com.elton.pixservice.domain.valueobject.Money;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawUseCase Tests")
class WithdrawUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private WithdrawUseCase withdrawUseCase;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = Wallet.builder()
                .id(1L)
                .userId("user123")
                .balance(Money.of(500.00))
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("Should withdraw successfully")
    void shouldWithdrawSuccessfully() {
        // Given
        Long walletId = 1L;
        Money amount = Money.of(100.00);

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Wallet result = withdrawUseCase.execute(walletId, amount);

        // Then
        assertNotNull(result);
        assertEquals(Money.of(400.00), result.getBalance());

        verify(walletRepository).findByIdWithLock(walletId);
        verify(walletRepository).save(any(Wallet.class));
        verify(ledgerEntryRepository).save(argThat(entry ->
            entry.getWalletId().equals(walletId) &&
            entry.getAmount().equals(amount) &&
            entry.getType() == LedgerEntryType.WITHDRAW
        ));
    }

    @Test
    @DisplayName("Should throw exception when wallet not found")
    void shouldThrowExceptionWhenWalletNotFound() {
        // Given
        Long walletId = 999L;
        Money amount = Money.of(100.00);

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.empty());

        // When & Then
        WalletNaoEncontradaException exception = assertThrows(
            WalletNaoEncontradaException.class,
            () -> withdrawUseCase.execute(walletId, amount)
        );
        assertTrue(exception.getMessage().contains("Wallet not found"));

        verify(walletRepository).findByIdWithLock(walletId);
        verify(walletRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when insufficient balance")
    void shouldThrowExceptionWhenInsufficientBalance() {
        // Given
        Long walletId = 1L;
        Money amount = Money.of(600.00); // More than balance

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));

        // When & Then
        SaldoInsuficienteException exception = assertThrows(
            SaldoInsuficienteException.class,
            () -> withdrawUseCase.execute(walletId, amount)
        );
        assertTrue(exception.getMessage().contains("Insufficient balance"));

        verify(walletRepository).findByIdWithLock(walletId);
        verify(walletRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create ledger entry after withdraw")
    void shouldCreateLedgerEntryAfterWithdraw() {
        // Given
        Long walletId = 1L;
        Money amount = Money.of(100.00);

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(i -> i.getArgument(0));

        // When
        withdrawUseCase.execute(walletId, amount);

        // Then
        verify(ledgerEntryRepository).save(argThat(entry ->
            entry.getWalletId().equals(walletId) &&
            entry.getAmount().equals(amount) &&
            entry.getType() == LedgerEntryType.WITHDRAW &&
            entry.getMetadata().equals("Withdraw operation")
        ));
    }

    @Test
    @DisplayName("Should use pessimistic locking")
    void shouldUsePessimisticLocking() {
        // Given
        Long walletId = 1L;
        Money amount = Money.of(100.00);

        when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(i -> i.getArgument(0));

        // When
        withdrawUseCase.execute(walletId, amount);

        // Then
        verify(walletRepository).findByIdWithLock(walletId);
        verify(walletRepository, never()).findById(anyLong());
    }
}

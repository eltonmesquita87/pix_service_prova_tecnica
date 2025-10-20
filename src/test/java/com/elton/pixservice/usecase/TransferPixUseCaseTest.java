package com.elton.pixservice.usecase;

import com.elton.pixservice.domain.entity.PixKey;
import com.elton.pixservice.domain.entity.PixTransfer;
import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.exception.RequisicaoDuplicadaException;
import com.elton.pixservice.domain.exception.SaldoInsuficienteException;
import com.elton.pixservice.domain.exception.TransferenciaInvalidaException;
import com.elton.pixservice.domain.exception.WalletNaoEncontradaException;
import com.elton.pixservice.domain.repository.*;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.domain.valueobject.PixKeyType;
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
@DisplayName("TransferPixUseCase Tests")
class TransferPixUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private PixTransferRepository pixTransferRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @InjectMocks
    private TransferPixUseCase transferPixUseCase;

    private Wallet sourceWallet;
    private Wallet destinationWallet;
    private PixKey pixKey;

    @BeforeEach
    void setUp() {
        sourceWallet = Wallet.builder()
                .id(1L)
                .userId("user1")
                .balance(Money.of(500.00))
                .version(0L)
                .build();

        destinationWallet = Wallet.builder()
                .id(2L)
                .userId("user2")
                .balance(Money.of(100.00))
                .version(0L)
                .build();

        pixKey = PixKey.builder()
                .id(1L)
                .walletId(2L)
                .keyType(PixKeyType.EMAIL)
                .keyValue("destination@example.com")
                .build();
    }

    @Test
    @DisplayName("Should transfer Pix successfully")
    void shouldTransferPixSuccessfully() {
        // Given
        Long fromWalletId = 1L;
        String pixKeyValue = "destination@example.com";
        Money amount = Money.of(150.00);
        String idempotencyKey = "unique-key-123";

        when(idempotencyRepository.exists(anyString(), anyString())).thenReturn(false);
        when(pixKeyRepository.findByKeyValue(pixKeyValue)).thenReturn(Optional.of(pixKey));
        when(walletRepository.findByIdWithLock(fromWalletId)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.existsById(2L)).thenReturn(true);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(pixTransferRepository.save(any(PixTransfer.class))).thenAnswer(i -> i.getArgument(0));
        when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        PixTransfer result = transferPixUseCase.execute(fromWalletId, pixKeyValue, amount, idempotencyKey);

        // Then
        assertNotNull(result);
        assertNotNull(result.getEndToEndId());
        assertTrue(result.getEndToEndId().startsWith("E"));
        assertEquals(fromWalletId, result.getFromWalletId());
        assertEquals(2L, result.getToWalletId());
        assertEquals(amount, result.getAmount());
        assertEquals(TransferStatus.PENDING, result.getStatus());

        verify(idempotencyRepository).exists("pix_transfer", idempotencyKey);
        verify(pixKeyRepository).findByKeyValue(pixKeyValue);
        verify(walletRepository).findByIdWithLock(fromWalletId);
        verify(walletRepository).save(any(Wallet.class));
        verify(pixTransferRepository).save(any(PixTransfer.class));
        verify(ledgerEntryRepository).save(any());
        verify(idempotencyRepository).saveIdempotencyKey(eq("pix_transfer"), eq(idempotencyKey), anyString());
    }

    @Test
    @DisplayName("Should throw exception when idempotency key already exists")
    void shouldThrowExceptionWhenIdempotencyKeyAlreadyExists() {
        // Given
        Long fromWalletId = 1L;
        String pixKeyValue = "destination@example.com";
        Money amount = Money.of(150.00);
        String idempotencyKey = "duplicate-key";

        when(idempotencyRepository.exists("pix_transfer", idempotencyKey)).thenReturn(true);

        // When & Then
        assertThrows(RequisicaoDuplicadaException.class, () ->
            transferPixUseCase.execute(fromWalletId, pixKeyValue, amount, idempotencyKey)
        );

        verify(idempotencyRepository).exists("pix_transfer", idempotencyKey);
        verify(pixKeyRepository, never()).findByKeyValue(anyString());
        verify(walletRepository, never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("Should throw exception when Pix key not found")
    void shouldThrowExceptionWhenPixKeyNotFound() {
        // Given
        Long fromWalletId = 1L;
        String pixKeyValue = "notfound@example.com";
        Money amount = Money.of(150.00);
        String idempotencyKey = "key-123";

        when(idempotencyRepository.exists(anyString(), anyString())).thenReturn(false);
        when(pixKeyRepository.findByKeyValue(pixKeyValue)).thenReturn(Optional.empty());

        // When & Then
        WalletNaoEncontradaException exception = assertThrows(
            WalletNaoEncontradaException.class,
            () -> transferPixUseCase.execute(fromWalletId, pixKeyValue, amount, idempotencyKey)
        );
        assertTrue(exception.getMessage().contains("Pix key not found"));

        verify(pixKeyRepository).findByKeyValue(pixKeyValue);
        verify(walletRepository, never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("Should throw exception when transferring to same wallet")
    void shouldThrowExceptionWhenTransferringToSameWallet() {
        // Given
        Long fromWalletId = 1L;
        String pixKeyValue = "same@example.com";
        Money amount = Money.of(150.00);
        String idempotencyKey = "key-123";

        PixKey sameWalletPixKey = PixKey.builder()
                .walletId(1L) // Same as source
                .keyValue(pixKeyValue)
                .build();

        when(idempotencyRepository.exists(anyString(), anyString())).thenReturn(false);
        when(pixKeyRepository.findByKeyValue(pixKeyValue)).thenReturn(Optional.of(sameWalletPixKey));

        // When & Then
        TransferenciaInvalidaException exception = assertThrows(
            TransferenciaInvalidaException.class,
            () -> transferPixUseCase.execute(fromWalletId, pixKeyValue, amount, idempotencyKey)
        );
        assertTrue(exception.getMessage().contains("Cannot transfer to the same wallet"));
    }

    @Test
    @DisplayName("Should throw exception when source wallet not found")
    void shouldThrowExceptionWhenSourceWalletNotFound() {
        // Given
        Long fromWalletId = 999L;
        String pixKeyValue = "destination@example.com";
        Money amount = Money.of(150.00);
        String idempotencyKey = "key-123";

        when(idempotencyRepository.exists(anyString(), anyString())).thenReturn(false);
        when(pixKeyRepository.findByKeyValue(pixKeyValue)).thenReturn(Optional.of(pixKey));
        when(walletRepository.findByIdWithLock(fromWalletId)).thenReturn(Optional.empty());

        // When & Then
        WalletNaoEncontradaException exception = assertThrows(
            WalletNaoEncontradaException.class,
            () -> transferPixUseCase.execute(fromWalletId, pixKeyValue, amount, idempotencyKey)
        );
        assertTrue(exception.getMessage().contains("Source wallet not found"));
    }

    @Test
    @DisplayName("Should throw exception when insufficient balance")
    void shouldThrowExceptionWhenInsufficientBalance() {
        // Given
        Long fromWalletId = 1L;
        String pixKeyValue = "destination@example.com";
        Money amount = Money.of(600.00); // More than balance
        String idempotencyKey = "key-123";

        when(idempotencyRepository.exists(anyString(), anyString())).thenReturn(false);
        when(pixKeyRepository.findByKeyValue(pixKeyValue)).thenReturn(Optional.of(pixKey));
        when(walletRepository.findByIdWithLock(fromWalletId)).thenReturn(Optional.of(sourceWallet));

        // When & Then
        SaldoInsuficienteException exception = assertThrows(
            SaldoInsuficienteException.class,
            () -> transferPixUseCase.execute(fromWalletId, pixKeyValue, amount, idempotencyKey)
        );
        assertTrue(exception.getMessage().contains("Insufficient balance"));

        verify(walletRepository, never()).save(any());
        verify(pixTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when destination wallet not found")
    void shouldThrowExceptionWhenDestinationWalletNotFound() {
        // Given
        Long fromWalletId = 1L;
        String pixKeyValue = "destination@example.com";
        Money amount = Money.of(150.00);
        String idempotencyKey = "key-123";

        when(idempotencyRepository.exists(anyString(), anyString())).thenReturn(false);
        when(pixKeyRepository.findByKeyValue(pixKeyValue)).thenReturn(Optional.of(pixKey));
        when(walletRepository.findByIdWithLock(fromWalletId)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.existsById(2L)).thenReturn(false);

        // When & Then
        WalletNaoEncontradaException exception = assertThrows(
            WalletNaoEncontradaException.class,
            () -> transferPixUseCase.execute(fromWalletId, pixKeyValue, amount, idempotencyKey)
        );
        assertTrue(exception.getMessage().contains("Destination wallet not found"));
    }

    @Test
    @DisplayName("Should debit source wallet immediately")
    void shouldDebitSourceWalletImmediately() {
        // Given
        Long fromWalletId = 1L;
        String pixKeyValue = "destination@example.com";
        Money amount = Money.of(150.00);
        String idempotencyKey = "key-123";

        when(idempotencyRepository.exists(anyString(), anyString())).thenReturn(false);
        when(pixKeyRepository.findByKeyValue(pixKeyValue)).thenReturn(Optional.of(pixKey));
        when(walletRepository.findByIdWithLock(fromWalletId)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.existsById(2L)).thenReturn(true);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(pixTransferRepository.save(any(PixTransfer.class))).thenAnswer(i -> i.getArgument(0));
        when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        transferPixUseCase.execute(fromWalletId, pixKeyValue, amount, idempotencyKey);

        // Then
        verify(walletRepository).save(argThat(wallet ->
            wallet.getId().equals(fromWalletId) &&
            wallet.getBalance().equals(Money.of(350.00))
        ));
    }

    @Test
    @DisplayName("Should use pessimistic locking")
    void shouldUsePessimisticLocking() {
        // Given
        Long fromWalletId = 1L;
        String pixKeyValue = "destination@example.com";
        Money amount = Money.of(150.00);
        String idempotencyKey = "key-123";

        when(idempotencyRepository.exists(anyString(), anyString())).thenReturn(false);
        when(pixKeyRepository.findByKeyValue(pixKeyValue)).thenReturn(Optional.of(pixKey));
        when(walletRepository.findByIdWithLock(fromWalletId)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.existsById(2L)).thenReturn(true);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(pixTransferRepository.save(any(PixTransfer.class))).thenAnswer(i -> i.getArgument(0));
        when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        transferPixUseCase.execute(fromWalletId, pixKeyValue, amount, idempotencyKey);

        // Then
        verify(walletRepository).findByIdWithLock(fromWalletId);
        verify(walletRepository, never()).findById(any());
    }
}

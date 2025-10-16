package com.elton.pixservice.usecase;

import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.repository.WalletRepository;
import com.elton.pixservice.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateWalletUseCase Tests")
class CreateWalletUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private CreateWalletUseCase createWalletUseCase;

    private Wallet savedWallet;

    @BeforeEach
    void setUp() {
        savedWallet = Wallet.builder()
                .id(1L)
                .userId("user123")
                .balance(Money.zero())
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("Should create wallet successfully")
    void shouldCreateWalletSuccessfully() {
        // Given
        String userId = "user123";
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        // When
        Wallet result = createWalletUseCase.execute(userId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("user123", result.getUserId());
        assertEquals(BigDecimal.ZERO.setScale(2), result.getBalance().getAmount());

        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should create wallet with zero balance")
    void shouldCreateWalletWithZeroBalance() {
        // Given
        String userId = "user456";
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        // When
        Wallet result = createWalletUseCase.execute(userId);

        // Then
        assertTrue(result.getBalance().isZero());
        verify(walletRepository).save(argThat(wallet ->
            wallet.getBalance().isZero() && wallet.getUserId().equals(userId)
        ));
    }

    @Test
    @DisplayName("Should call repository with correct parameters")
    void shouldCallRepositoryWithCorrectParameters() {
        // Given
        String userId = "testUser";
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        // When
        createWalletUseCase.execute(userId);

        // Then
        verify(walletRepository).save(argThat(wallet ->
            wallet.getUserId().equals(userId) &&
            wallet.getBalance().equals(Money.zero()) &&
            wallet.getVersion().equals(0L)
        ));
    }
}

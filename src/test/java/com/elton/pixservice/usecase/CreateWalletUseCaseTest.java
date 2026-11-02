package com.elton.pixservice.usecase;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.exception.CpfJaCadastradoException;
import com.elton.pixservice.domain.repository.WalletRepository;
import com.elton.pixservice.domain.valueobject.Cpf;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.domain.valueobject.WalletHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

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

    // ========== Wallet holder data ==========

    private static final String CPF_DIGITS = "12345678909";
    private static final String EMAIL = "maria.silva@example.com";
    private static final String PHONE = "+5511999999999";

    private ListAppender<ILoggingEvent> attachLogAppender() {
        Logger useCaseLogger = (Logger) LoggerFactory.getLogger(CreateWalletUseCase.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        useCaseLogger.addAppender(appender);
        return appender;
    }

    private void detachLogAppender(ListAppender<ILoggingEvent> appender) {
        Logger useCaseLogger = (Logger) LoggerFactory.getLogger(CreateWalletUseCase.class);
        useCaseLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    @DisplayName("WHOLD-10: Should throw CpfJaCadastradoException and never save when the CPF already exists")
    void shouldRejectDuplicateCpfWithoutSaving() {
        // Given
        WalletHolder holder = WalletHolder.of(CPF_DIGITS, null, null, null);
        when(walletRepository.existsByCpf(Cpf.of(CPF_DIGITS))).thenReturn(true);

        // When
        CpfJaCadastradoException exception = assertThrows(CpfJaCadastradoException.class,
                () -> createWalletUseCase.execute("user123", holder));

        // Then
        assertEquals("CPF already registered", exception.getMessage());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("WHOLD-01: Should save wallet with the given holder, zero balance and version 0 when the CPF is new")
    void shouldSaveWalletWithHolderWhenCpfIsNew() {
        // Given
        WalletHolder holder = WalletHolder.of(CPF_DIGITS, "Maria Silva", EMAIL, PHONE);
        when(walletRepository.existsByCpf(Cpf.of(CPF_DIGITS))).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Wallet result = createWalletUseCase.execute("user123", holder);

        // Then
        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());
        Wallet saved = captor.getValue();
        assertEquals(holder, saved.getHolder());
        assertEquals("user123", saved.getUserId());
        assertTrue(saved.getBalance().isZero());
        assertEquals(0L, saved.getVersion());
        assertEquals(holder, result.getHolder());
    }

    @Test
    @DisplayName("WHOLD-01: Should not check CPF uniqueness when the holder has no CPF")
    void shouldNotCheckCpfWhenHolderHasNoCpf() {
        // Given
        WalletHolder holder = WalletHolder.of(null, "Maria Silva", EMAIL, PHONE);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Wallet result = createWalletUseCase.execute("user123", holder);

        // Then
        verify(walletRepository, never()).existsByCpf(any(Cpf.class));
        assertEquals(holder, result.getHolder());
        assertNull(result.getHolder().getCpf());
    }

    @Test
    @DisplayName("WHOLD-13: Should save an empty holder when only the userId is given")
    void shouldSaveEmptyHolderWhenOnlyUserIdIsGiven() {
        // Given
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Wallet result = createWalletUseCase.execute("user123");

        // Then
        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());
        assertEquals(WalletHolder.empty(), captor.getValue().getHolder());
        assertEquals(WalletHolder.empty(), result.getHolder());
        verify(walletRepository, never()).existsByCpf(any(Cpf.class));
    }

    @Test
    @DisplayName("WHOLD-18: Should never log the CPF, e-mail or phone when creating a wallet")
    void shouldNotLogHolderDataOnCreate() {
        // Given
        WalletHolder holder = WalletHolder.of(CPF_DIGITS, "Maria Silva", EMAIL, PHONE);
        when(walletRepository.existsByCpf(any(Cpf.class))).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ListAppender<ILoggingEvent> appender = attachLogAppender();

        try {
            // When
            createWalletUseCase.execute("user123", holder);

            // Then
            assertFalse(appender.list.isEmpty(), "the use case is expected to log, otherwise this test proves nothing");
            for (ILoggingEvent event : appender.list) {
                String message = event.getFormattedMessage();
                assertFalse(message.contains(CPF_DIGITS), message);
                assertFalse(message.contains("123.456.789-09"), message);
                assertFalse(message.contains(EMAIL), message);
                assertFalse(message.contains(PHONE), message);
            }
        } finally {
            detachLogAppender(appender);
        }
    }

    @Test
    @DisplayName("WHOLD-18: Should never log the CPF, e-mail or phone when the CPF is a duplicate")
    void shouldNotLogHolderDataOnDuplicateCpf() {
        // Given
        WalletHolder holder = WalletHolder.of(CPF_DIGITS, "Maria Silva", EMAIL, PHONE);
        when(walletRepository.existsByCpf(any(Cpf.class))).thenReturn(true);
        ListAppender<ILoggingEvent> appender = attachLogAppender();

        try {
            // When
            assertThrows(CpfJaCadastradoException.class, () -> createWalletUseCase.execute("user123", holder));

            // Then
            for (ILoggingEvent event : appender.list) {
                String message = event.getFormattedMessage();
                assertFalse(message.contains(CPF_DIGITS), message);
                assertFalse(message.contains(EMAIL), message);
                assertFalse(message.contains(PHONE), message);
            }
        } finally {
            detachLogAppender(appender);
        }
    }
}

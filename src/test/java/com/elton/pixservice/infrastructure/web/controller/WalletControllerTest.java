package com.elton.pixservice.infrastructure.web.controller;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.exception.CpfJaCadastradoException;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.domain.valueobject.WalletHolder;
import com.elton.pixservice.infrastructure.exception.GlobalExceptionHandler;
import com.elton.pixservice.usecase.CreateWalletUseCase;
import com.elton.pixservice.usecase.DepositUseCase;
import com.elton.pixservice.usecase.GetBalanceUseCase;
import com.elton.pixservice.usecase.RegisterPixKeyUseCase;
import com.elton.pixservice.usecase.WithdrawUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletController Tests")
class WalletControllerTest {

    private static final String CPF_MASKED = "***.456.789-**";

    @Mock
    private CreateWalletUseCase createWalletUseCase;
    @Mock
    private RegisterPixKeyUseCase registerPixKeyUseCase;
    @Mock
    private DepositUseCase depositUseCase;
    @Mock
    private WithdrawUseCase withdrawUseCase;
    @Mock
    private GetBalanceUseCase getBalanceUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WalletController controller = new WalletController(
                createWalletUseCase, registerPixKeyUseCase, depositUseCase, withdrawUseCase, getBalanceUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void useCaseReturnsWalletWithGivenHolder() {
        when(createWalletUseCase.execute(anyString(), any(WalletHolder.class))).thenAnswer(invocation ->
                Wallet.builder()
                        .id(1L)
                        .userId(invocation.getArgument(0))
                        .balance(Money.zero())
                        .version(0L)
                        .holder(invocation.getArgument(1))
                        .build());
    }

    // ========== POST /wallets: success ==========

    @Test
    @DisplayName("WHOLD-01/16/17: full valid body gives 201 with masked CPF and normalized nome, email and telefone")
    void shouldCreateWalletWithFullHolder() throws Exception {
        useCaseReturnsWalletWithGivenHolder();
        String body = "{\"userId\":\"user123\",\"cpf\":\"123.456.789-09\",\"nome\":\"  Maria Silva  \","
                + "\"email\":\"  Maria.Silva@Example.COM \",\"telefone\":\"+5511999999999\"}";

        mockMvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.cpf").value(CPF_MASKED))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("maria.silva@example.com"))
                .andExpect(jsonPath("$.telefone").value("+5511999999999"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("12345678909"))));

        ArgumentCaptor<WalletHolder> holder = ArgumentCaptor.forClass(WalletHolder.class);
        verify(createWalletUseCase).execute(eq("user123"), holder.capture());
        assertEquals(WalletHolder.of("12345678909", "Maria Silva", "maria.silva@example.com", "+5511999999999"),
                holder.getValue());
    }

    @Test
    @DisplayName("WHOLD-01: a subset of the holder fields gives 201 with only those fields in the response")
    void shouldCreateWalletWithHolderSubset() throws Exception {
        useCaseReturnsWalletWithGivenHolder();

        mockMvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user123\",\"email\":\"maria@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("maria@example.com"))
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.nome").doesNotExist())
                .andExpect(jsonPath("$.telefone").doesNotExist());
    }

    @Test
    @DisplayName("WHOLD-13: body with only userId gives 201, balance 0 and no holder keys")
    void shouldCreateWalletWithOnlyUserId() throws Exception {
        useCaseReturnsWalletWithGivenHolder();

        mockMvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.nome").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telefone").doesNotExist());

        verify(createWalletUseCase).execute("user123", WalletHolder.empty());
    }

    @Test
    @DisplayName("Edge case (explicit null): null holder fields are treated as absent")
    void shouldTreatExplicitNullAsAbsent() throws Exception {
        useCaseReturnsWalletWithGivenHolder();

        mockMvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user123\",\"cpf\":null,\"nome\":null,\"email\":null,\"telefone\":null}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").doesNotExist());

        verify(createWalletUseCase).execute("user123", WalletHolder.empty());
    }

    // ========== POST /wallets: invalid holder data ==========

    static Stream<Arguments> invalidHolderFields() {
        return Stream.of(
                Arguments.of("WHOLD-02", "cpf", "123.456.789-00", "Invalid CPF"),
                Arguments.of("WHOLD-04", "email", "not-an-email", "Invalid email"),
                Arguments.of("WHOLD-06", "telefone", "11999999999", "Invalid phone"),
                Arguments.of("WHOLD-07", "nome", "A", "Invalid name"),
                Arguments.of("WHOLD-09", "cpf", "   ", "Invalid CPF"),
                Arguments.of("WHOLD-09", "email", "   ", "Invalid email"),
                Arguments.of("WHOLD-09", "telefone", "   ", "Invalid phone"),
                Arguments.of("WHOLD-09", "nome", "   ", "Invalid name"));
    }

    @ParameterizedTest(name = "{0}: {1} = \"{2}\" gives 400 {3}")
    @MethodSource("invalidHolderFields")
    @DisplayName("WHOLD-02/04/06/07/09: invalid or blank holder field gives 400 with its exact message and no wallet")
    void shouldRejectInvalidHolderField(String requirement, String field, String value, String message)
            throws Exception {
        String body = "{\"userId\":\"user123\",\"" + field + "\":\"" + value + "\"}";

        mockMvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("/wallets"));

        verifyNoInteractions(createWalletUseCase);
    }

    @Test
    @DisplayName("WHOLD-19: the error body for an invalid CPF does not contain the CPF sent")
    void shouldNotEchoInvalidCpfInErrorBody() throws Exception {
        mockMvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user123\",\"cpf\":\"123.456.789-00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("123.456.789-00"))));
    }

    @Test
    @DisplayName("WHOLD-10: use case throwing CpfJaCadastradoException gives 409 'CPF already registered'")
    void shouldReturn409WhenCpfAlreadyRegistered() throws Exception {
        when(createWalletUseCase.execute(anyString(), any(WalletHolder.class)))
                .thenThrow(new CpfJaCadastradoException());

        mockMvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user123\",\"cpf\":\"12345678909\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("CPF already registered"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("12345678909"))));
    }

    // ========== POST /wallets: userId and wrong JSON type ==========

    @Test
    @DisplayName("WHOLD-14: empty userId gives 400 'User ID is required' and no wallet")
    void shouldRejectEmptyUserId() throws Exception {
        mockMvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User ID is required"));

        verifyNoInteractions(createWalletUseCase);
    }

    @Test
    @DisplayName("WHOLD-14: missing userId gives 400 'User ID is required' and no wallet")
    void shouldRejectMissingUserId() throws Exception {
        mockMvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"12345678909\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User ID is required"));

        verifyNoInteractions(createWalletUseCase);
    }

    @Test
    @DisplayName("Edge case (cpf with wrong JSON type): cpf as a JSON number gives 400 'Malformed request body'")
    void shouldRejectCpfAsJsonNumber() throws Exception {
        mockMvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user123\",\"cpf\":12345678909}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("12345678909"))));

        verifyNoInteractions(createWalletUseCase);
    }

    // ========== Logs ==========

    @Test
    @DisplayName("WHOLD-18: creating a wallet logs no CPF, e-mail, phone or name from the controller")
    void shouldNotLogHolderDataInController() throws Exception {
        useCaseReturnsWalletWithGivenHolder();
        Logger controllerLogger = (Logger) LoggerFactory.getLogger(WalletController.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        controllerLogger.addAppender(appender);

        try {
            mockMvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"user123\",\"cpf\":\"12345678909\",\"nome\":\"Maria Silva\","
                                    + "\"email\":\"maria@example.com\",\"telefone\":\"+5511999999999\"}"))
                    .andExpect(status().isCreated());

            assertFalse(appender.list.isEmpty(), "the controller is expected to log, otherwise this test proves nothing");
            for (ILoggingEvent event : appender.list) {
                String message = event.getFormattedMessage();
                assertFalse(message.contains("12345678909"), message);
                assertFalse(message.contains("123.456.789-09"), message);
                assertFalse(message.contains("Maria Silva"), message);
                assertFalse(message.contains("maria@example.com"), message);
                assertFalse(message.contains("+5511999999999"), message);
            }
        } finally {
            controllerLogger.detachAppender(appender);
            appender.stop();
        }
    }

    // ========== Deposit and withdraw ==========

    private Wallet walletWithFullHolder() {
        return Wallet.builder()
                .id(1L)
                .userId("user123")
                .balance(Money.of(new BigDecimal("50.00")))
                .version(1L)
                .holder(WalletHolder.of("12345678909", "Maria Silva", "maria@example.com", "+5511999999999"))
                .build();
    }

    @Test
    @DisplayName("WHOLD-20: deposit response carries no holder keys even when the wallet has a holder")
    void shouldNotReturnHolderInDepositResponse() throws Exception {
        when(depositUseCase.execute(anyLong(), any(Money.class))).thenReturn(walletWithFullHolder());

        mockMvc.perform(post("/wallets/1/deposit").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.balance").value(50.0))
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.nome").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telefone").doesNotExist());
    }

    @Test
    @DisplayName("WHOLD-20: withdraw response carries no holder keys even when the wallet has a holder")
    void shouldNotReturnHolderInWithdrawResponse() throws Exception {
        when(withdrawUseCase.execute(anyLong(), any(Money.class))).thenReturn(walletWithFullHolder());

        mockMvc.perform(post("/wallets/1/withdraw").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.balance").value(50.0))
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.nome").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telefone").doesNotExist());
    }
}

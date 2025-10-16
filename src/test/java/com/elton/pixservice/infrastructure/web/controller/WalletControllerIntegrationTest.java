package com.elton.pixservice.infrastructure.web.controller;

import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.infrastructure.web.dto.CreateWalletRequest;
import com.elton.pixservice.infrastructure.web.dto.DepositRequest;
import com.elton.pixservice.infrastructure.web.dto.RegisterPixKeyRequest;
import com.elton.pixservice.domain.valueobject.PixKeyType;
import com.elton.pixservice.usecase.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@DisplayName("WalletController Integration Tests")
@Disabled
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateWalletUseCase createWalletUseCase;

    @MockBean
    private RegisterPixKeyUseCase registerPixKeyUseCase;

    @MockBean
    private DepositUseCase depositUseCase;

    @MockBean
    private WithdrawUseCase withdrawUseCase;

    @MockBean
    private GetBalanceUseCase getBalanceUseCase;

    @Test
    @DisplayName("POST /wallets should create wallet successfully")
    void shouldCreateWalletSuccessfully() throws Exception {
        // Given
        CreateWalletRequest request = new CreateWalletRequest("user123");
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId("user123")
                .balance(Money.zero())
                .version(0L)
                .build();

        when(createWalletUseCase.execute("user123")).thenReturn(wallet);

        // When & Then
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.balance").value(0.00));
    }

    @Test
    @DisplayName("POST /wallets should return 400 when userId is empty")
    void shouldReturn400WhenUserIdIsEmpty() throws Exception {
        // Given
        CreateWalletRequest request = new CreateWalletRequest("");

        // When & Then
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /wallets/{id}/pix-keys should register Pix key successfully")
    void shouldRegisterPixKeySuccessfully() throws Exception {
        // Given
        RegisterPixKeyRequest request = new RegisterPixKeyRequest(
            PixKeyType.EMAIL,
            "user@example.com"
        );

        // When & Then
        mockMvc.perform(post("/wallets/1/pix-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /wallets/{id}/deposit should deposit successfully")
    void shouldDepositSuccessfully() throws Exception {
        // Given
        DepositRequest request = new DepositRequest(new BigDecimal("100.00"));
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId("user123")
                .balance(Money.of(100.00))
                .build();

        when(depositUseCase.execute(eq(1L), any(Money.class))).thenReturn(wallet);

        // When & Then
        mockMvc.perform(post("/wallets/1/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    @DisplayName("POST /wallets/{id}/deposit should return 400 for negative amount")
    void shouldReturn400ForNegativeDeposit() throws Exception {
        // Given
        DepositRequest request = new DepositRequest(new BigDecimal("-10.00"));

        // When & Then
        mockMvc.perform(post("/wallets/1/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance should return current balance")
    void shouldReturnCurrentBalance() throws Exception {
        // Given
        when(getBalanceUseCase.getCurrentBalance(1L)).thenReturn(Money.of(250.00));

        // When & Then
        mockMvc.perform(get("/wallets/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(1))
                .andExpect(jsonPath("$.balance").value(250.00));
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance with timestamp should return historical balance")
    void shouldReturnHistoricalBalance() throws Exception {
        // Given
        when(getBalanceUseCase.getHistoricalBalance(eq(1L), any()))
                .thenReturn(Money.of(200.00));

        // When & Then
        mockMvc.perform(get("/wallets/1/balance")
                .param("at", "2025-10-13T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(200.00));
    }
}

package com.elton.pixservice;

import com.elton.pixservice.domain.valueobject.PixKeyType;
import com.elton.pixservice.infrastructure.web.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Pix Transfer End-to-End Test")
@Disabled
class PixTransferEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should complete full Pix transfer flow successfully")
    void shouldCompleteFullPixTransferFlowSuccessfully() throws Exception {
        // STEP 1: Create source wallet
        CreateWalletRequest aliceRequest = new CreateWalletRequest("alice");
        MvcResult aliceResult = mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliceRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("alice"))
                .andExpect(jsonPath("$.balance").value(0.00))
                .andReturn();

        WalletResponse alice = objectMapper.readValue(
            aliceResult.getResponse().getContentAsString(),
            WalletResponse.class
        );
        Long aliceWalletId = alice.getId();
        assertNotNull(aliceWalletId);

        // STEP 2: Create destination wallet
        CreateWalletRequest bobRequest = new CreateWalletRequest("bob");
        MvcResult bobResult = mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bobRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("bob"))
                .andExpect(jsonPath("$.balance").value(0.00))
                .andReturn();

        WalletResponse bob = objectMapper.readValue(
            bobResult.getResponse().getContentAsString(),
            WalletResponse.class
        );
        Long bobWalletId = bob.getId();
        assertNotNull(bobWalletId);

        // STEP 3: Register Pix key for Bob
        RegisterPixKeyRequest pixKeyRequest = new RegisterPixKeyRequest(
            PixKeyType.EMAIL,
            "bob@example.com"
        );
        mockMvc.perform(post("/wallets/" + bobWalletId + "/pix-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pixKeyRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.keyValue").value("bob@example.com"));

        // STEP 4: Deposit money into Alice's wallet
        DepositRequest depositRequest = new DepositRequest(new BigDecimal("500.00"));
        mockMvc.perform(post("/wallets/" + aliceWalletId + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));

        // Verify Alice's balance
        mockMvc.perform(get("/wallets/" + aliceWalletId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));

        // STEP 5: Alice transfers to Bob via Pix
        TransferPixRequest transferRequest = new TransferPixRequest(
            aliceWalletId,
            "bob@example.com",
            new BigDecimal("150.00")
        );
        MvcResult transferResult = mockMvc.perform(post("/pix/transfers")
                .header("Idempotency-Key", "unique-transfer-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.fromWalletId").value(aliceWalletId))
                .andExpect(jsonPath("$.toWalletId").value(bobWalletId))
                .andReturn();

        TransferPixResponse transferResponse = objectMapper.readValue(
            transferResult.getResponse().getContentAsString(),
            TransferPixResponse.class
        );
        String endToEndId = transferResponse.getEndToEndId();
        assertNotNull(endToEndId);
        assertTrue(endToEndId.startsWith("E"));

        // STEP 6: Verify Alice's balance after transfer (debited immediately)
        mockMvc.perform(get("/wallets/" + aliceWalletId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(350.00));

        // STEP 7: Verify Bob's balance (not credited yet, waiting for confirmation)
        mockMvc.perform(get("/wallets/" + bobWalletId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0.00));

        // STEP 8: Process webhook to confirm transfer
        WebhookRequest webhookRequest = new WebhookRequest(
            endToEndId,
            "evt_confirm_001",
            "CONFIRMED",
            java.time.LocalDateTime.now()
        );
        mockMvc.perform(post("/pix/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookRequest)))
                .andExpect(status().isOk());

        // STEP 9: Verify final balances
        // Alice: 500 - 150 = 350
        mockMvc.perform(get("/wallets/" + aliceWalletId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(350.00));

        // Bob: 0 + 150 = 150 (now credited!)
        mockMvc.perform(get("/wallets/" + bobWalletId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    @DisplayName("Should handle rejected transfer correctly")
    void shouldHandleRejectedTransferCorrectly() throws Exception {
        // Create wallet and deposit
        CreateWalletRequest request = new CreateWalletRequest("user1");
        MvcResult walletResult = mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        WalletResponse wallet = objectMapper.readValue(
            walletResult.getResponse().getContentAsString(),
            WalletResponse.class
        );
        Long walletId = wallet.getId();

        // Deposit
        DepositRequest depositRequest = new DepositRequest(new BigDecimal("300.00"));
        mockMvc.perform(post("/wallets/" + walletId + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk());

        // Create destination and register Pix key
        CreateWalletRequest destRequest = new CreateWalletRequest("user2");
        MvcResult destResult = mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(destRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        WalletResponse destWallet = objectMapper.readValue(
            destResult.getResponse().getContentAsString(),
            WalletResponse.class
        );

        RegisterPixKeyRequest pixKeyRequest = new RegisterPixKeyRequest(
            PixKeyType.EMAIL,
            "dest@example.com"
        );
        mockMvc.perform(post("/wallets/" + destWallet.getId() + "/pix-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pixKeyRequest)))
                .andExpect(status().isCreated());

        // Transfer
        TransferPixRequest transferRequest = new TransferPixRequest(
            walletId,
            "dest@example.com",
            new BigDecimal("100.00")
        );
        MvcResult transferResult = mockMvc.perform(post("/pix/transfers")
                .header("Idempotency-Key", "reject-transfer-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        TransferPixResponse transferResponse = objectMapper.readValue(
            transferResult.getResponse().getContentAsString(),
            TransferPixResponse.class
        );

        // Balance after transfer (debited)
        mockMvc.perform(get("/wallets/" + walletId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(200.00));

        // Webhook REJECTED
        WebhookRequest webhookRequest = new WebhookRequest(
            transferResponse.getEndToEndId(),
            "evt_reject_001",
            "REJECTED",
            java.time.LocalDateTime.now()
        );
        mockMvc.perform(post("/pix/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookRequest)))
                .andExpect(status().isOk());

        // Balance after rejection (refunded)
        mockMvc.perform(get("/wallets/" + walletId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(300.00));

        // Destination should still be zero
        mockMvc.perform(get("/wallets/" + destWallet.getId() + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0.00));
    }

    @Test
    @DisplayName("Should enforce idempotency on duplicate transfers")
    void shouldEnforceIdempotencyOnDuplicateTransfers() throws Exception {
        // Setup wallets
        CreateWalletRequest sourceRequest = new CreateWalletRequest("source");
        MvcResult sourceResult = mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sourceRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        WalletResponse source = objectMapper.readValue(
            sourceResult.getResponse().getContentAsString(),
            WalletResponse.class
        );

        CreateWalletRequest destRequest = new CreateWalletRequest("dest");
        MvcResult destResult = mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(destRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        WalletResponse dest = objectMapper.readValue(
            destResult.getResponse().getContentAsString(),
            WalletResponse.class
        );

        RegisterPixKeyRequest pixKeyRequest = new RegisterPixKeyRequest(
            PixKeyType.EMAIL,
            "idempotency@example.com"
        );
        mockMvc.perform(post("/wallets/" + dest.getId() + "/pix-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pixKeyRequest)))
                .andExpect(status().isCreated());

        // Deposit
        DepositRequest depositRequest = new DepositRequest(new BigDecimal("200.00"));
        mockMvc.perform(post("/wallets/" + source.getId() + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk());

        // First transfer
        TransferPixRequest transferRequest = new TransferPixRequest(
            source.getId(),
            "idempotency@example.com",
            new BigDecimal("50.00")
        );
        mockMvc.perform(post("/pix/transfers")
                .header("Idempotency-Key", "duplicate-key-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated());

        // Second transfer with same idempotency key (should fail)
        mockMvc.perform(post("/pix/transfers")
                .header("Idempotency-Key", "duplicate-key-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isConflict());

        // Balance should reflect only one debit
        mockMvc.perform(get("/wallets/" + source.getId() + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }
}

package com.elton.pixservice.infrastructure.web.controller;

import com.elton.pixservice.domain.valueobject.PixKeyType;
import com.elton.pixservice.infrastructure.BaseIntegrationTest;
import com.elton.pixservice.infrastructure.web.dto.*;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("PixController Integration Tests with REST Assured")
@Disabled
class PixControllerRestAssuredTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /pix/transfers should transfer successfully")
    void shouldTransferSuccessfully() {
        // Setup: Create two wallets
        Integer sourceWalletId = createWallet("alice");
        Integer destWalletId = createWallet("bob");

        // Register Pix key for destination
        registerPixKey(destWalletId, PixKeyType.EMAIL, "bob@example.com");

        // Deposit into source wallet
        deposit(sourceWalletId, new BigDecimal("500.00"));

        // Transfer
        TransferPixRequest transferRequest = new TransferPixRequest(
            sourceWalletId.longValue(),
            "bob@example.com",
            new BigDecimal("150.00")
        );

        String idempotencyKey = "transfer-" + UUID.randomUUID();

        given()
            .spec(requestSpec)
            .header("Idempotency-Key", idempotencyKey)
            .body(transferRequest)
        .when()
            .post("/pix/transfers")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("endToEndId", notNullValue())
            .body("endToEndId", startsWith("E"))
            .body("fromWalletId", equalTo(sourceWalletId))
            .body("toWalletId", equalTo(destWalletId))
            .body("amount", equalTo(150.00f))
            .body("status", equalTo("PENDING"));

        // Verify source wallet was debited immediately
        given()
            .spec(requestSpec)
        .when()
            .get("/wallets/{id}/balance", sourceWalletId)
        .then()
            .body("balance", equalTo(350.00f));

        // Verify destination wallet not credited yet (waiting for webhook)
        given()
            .spec(requestSpec)
        .when()
            .get("/wallets/{id}/balance", destWalletId)
        .then()
            .body("balance", equalTo(0.00f));
    }

    @Test
    @DisplayName("POST /pix/transfers should return 400 when missing Idempotency-Key")
    void shouldReturn400WhenMissingIdempotencyKey() {
        Integer sourceWalletId = createWallet("user1");
        Integer destWalletId = createWallet("user2");
        registerPixKey(destWalletId, PixKeyType.EMAIL, "dest@example.com");
        deposit(sourceWalletId, new BigDecimal("100.00"));

        TransferPixRequest transferRequest = new TransferPixRequest(
            sourceWalletId.longValue(),
            "dest@example.com",
            new BigDecimal("50.00")
        );

        given()
            .spec(requestSpec)
            // No Idempotency-Key header
            .body(transferRequest)
        .when()
            .post("/pix/transfers")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("POST /pix/transfers should return 409 for duplicate idempotency key")
    void shouldReturn409ForDuplicateIdempotencyKey() {
        Integer sourceWalletId = createWallet("duplicateUser");
        Integer destWalletId = createWallet("destUser");
        registerPixKey(destWalletId, PixKeyType.EMAIL, "dest@example.com");
        deposit(sourceWalletId, new BigDecimal("300.00"));

        TransferPixRequest transferRequest = new TransferPixRequest(
            sourceWalletId.longValue(),
            "dest@example.com",
            new BigDecimal("100.00")
        );

        String idempotencyKey = "duplicate-key-" + UUID.randomUUID();

        // First transfer
        given()
            .spec(requestSpec)
            .header("Idempotency-Key", idempotencyKey)
            .body(transferRequest)
        .when()
            .post("/pix/transfers")
        .then()
            .statusCode(HttpStatus.CREATED.value());

        // Second transfer with same key
        given()
            .spec(requestSpec)
            .header("Idempotency-Key", idempotencyKey)
            .body(transferRequest)
        .when()
            .post("/pix/transfers")
        .then()
            .statusCode(HttpStatus.CONFLICT.value())
            .body("message", equalTo("Duplicate request"));

        // Balance should reflect only one debit
        given()
            .spec(requestSpec)
        .when()
            .get("/wallets/{id}/balance", sourceWalletId)
        .then()
            .body("balance", equalTo(200.00f));
    }

    @Test
    @DisplayName("POST /pix/transfers should return 400 for invalid Pix key")
    void shouldReturn400ForInvalidPixKey() {
        Integer sourceWalletId = createWallet("sender");
        deposit(sourceWalletId, new BigDecimal("100.00"));

        TransferPixRequest transferRequest = new TransferPixRequest(
            sourceWalletId.longValue(),
            "nonexistent@example.com",
            new BigDecimal("50.00")
        );

        given()
            .spec(requestSpec)
            .header("Idempotency-Key", "invalid-key-" + UUID.randomUUID())
            .body(transferRequest)
        .when()
            .post("/pix/transfers")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("Pix key not found"));
    }

    @Test
    @DisplayName("POST /pix/transfers should return 409 for insufficient balance")
    void shouldReturn409ForInsufficientBalance() {
        Integer sourceWalletId = createWallet("poorUser");
        Integer destWalletId = createWallet("richUser");
        registerPixKey(destWalletId, PixKeyType.EMAIL, "rich@example.com");
        deposit(sourceWalletId, new BigDecimal("50.00"));

        TransferPixRequest transferRequest = new TransferPixRequest(
            sourceWalletId.longValue(),
            "rich@example.com",
            new BigDecimal("100.00")
        );

        given()
            .spec(requestSpec)
            .header("Idempotency-Key", "insufficient-" + UUID.randomUUID())
            .body(transferRequest)
        .when()
            .post("/pix/transfers")
        .then()
            .statusCode(HttpStatus.CONFLICT.value())
            .body("message", equalTo("Insufficient balance"));
    }

    @Test
    @DisplayName("POST /pix/webhook should process CONFIRMED event")
    void shouldProcessConfirmedEvent() {
        // Setup transfer
        Integer sourceWalletId = createWallet("webhookSender");
        Integer destWalletId = createWallet("webhookReceiver");
        registerPixKey(destWalletId, PixKeyType.EMAIL, "receiver@example.com");
        deposit(sourceWalletId, new BigDecimal("400.00"));

        // Create transfer
        TransferPixRequest transferRequest = new TransferPixRequest(
            sourceWalletId.longValue(),
            "receiver@example.com",
            new BigDecimal("200.00")
        );

        String endToEndId = given()
            .spec(requestSpec)
            .header("Idempotency-Key", "webhook-transfer-" + UUID.randomUUID())
            .body(transferRequest)
        .when()
            .post("/pix/transfers")
        .then()
            .extract()
            .path("endToEndId");

        // Send CONFIRMED webhook
        WebhookRequest webhookRequest = new WebhookRequest(
            endToEndId,
            "evt-confirm-" + UUID.randomUUID(),
            "CONFIRMED",
            LocalDateTime.now()
        );

        given()
            .spec(requestSpec)
            .body(webhookRequest)
        .when()
            .post("/pix/webhook")
        .then()
            .statusCode(HttpStatus.OK.value());

        // Verify destination wallet was credited
        given()
            .spec(requestSpec)
        .when()
            .get("/wallets/{id}/balance", destWalletId)
        .then()
            .body("balance", equalTo(200.00f));

        // Source balance should remain debited
        given()
            .spec(requestSpec)
        .when()
            .get("/wallets/{id}/balance", sourceWalletId)
        .then()
            .body("balance", equalTo(200.00f));
    }

    @Test
    @DisplayName("POST /pix/webhook should process REJECTED event with refund")
    void shouldProcessRejectedEventWithRefund() {
        // Setup transfer
        Integer sourceWalletId = createWallet("refundUser");
        Integer destWalletId = createWallet("destUser");
        registerPixKey(destWalletId, PixKeyType.EMAIL, "dest@example.com");
        deposit(sourceWalletId, new BigDecimal("300.00"));

        // Create transfer
        TransferPixRequest transferRequest = new TransferPixRequest(
            sourceWalletId.longValue(),
            "dest@example.com",
            new BigDecimal("100.00")
        );

        String endToEndId = given()
            .spec(requestSpec)
            .header("Idempotency-Key", "reject-transfer-" + UUID.randomUUID())
            .body(transferRequest)
        .when()
            .post("/pix/transfers")
        .then()
            .extract()
            .path("endToEndId");

        // Verify debit
        given()
            .spec(requestSpec)
        .when()
            .get("/wallets/{id}/balance", sourceWalletId)
        .then()
            .body("balance", equalTo(200.00f));

        // Send REJECTED webhook
        WebhookRequest webhookRequest = new WebhookRequest(
            endToEndId,
            "evt-reject-" + UUID.randomUUID(),
            "REJECTED",
            LocalDateTime.now()
        );

        given()
            .spec(requestSpec)
            .body(webhookRequest)
        .when()
            .post("/pix/webhook")
        .then()
            .statusCode(HttpStatus.OK.value());

        // Verify refund
        given()
            .spec(requestSpec)
        .when()
            .get("/wallets/{id}/balance", sourceWalletId)
        .then()
            .body("balance", equalTo(300.00f));

        // Destination should remain zero
        given()
            .spec(requestSpec)
        .when()
            .get("/wallets/{id}/balance", destWalletId)
        .then()
            .body("balance", equalTo(0.00f));
    }

    @Test
    @DisplayName("POST /pix/webhook should be idempotent for duplicate events")
    void shouldBeIdempotentForDuplicateEvents() {
        // Setup and transfer
        Integer sourceWalletId = createWallet("idempotentSender");
        Integer destWalletId = createWallet("idempotentReceiver");
        registerPixKey(destWalletId, PixKeyType.EMAIL, "receiver@example.com");
        deposit(sourceWalletId, new BigDecimal("500.00"));

        TransferPixRequest transferRequest = new TransferPixRequest(
            sourceWalletId.longValue(),
            "receiver@example.com",
            new BigDecimal("150.00")
        );

        String endToEndId = given()
            .spec(requestSpec)
            .header("Idempotency-Key", "idempotent-" + UUID.randomUUID())
            .body(transferRequest)
        .when()
            .post("/pix/transfers")
        .then()
            .extract()
            .path("endToEndId");

        // Send webhook
        String eventId = "evt-duplicate-" + UUID.randomUUID();
        WebhookRequest webhookRequest = new WebhookRequest(
            endToEndId,
            eventId,
            "CONFIRMED",
            LocalDateTime.now()
        );

        // First webhook call
        given()
            .spec(requestSpec)
            .body(webhookRequest)
        .when()
            .post("/pix/webhook")
        .then()
            .statusCode(HttpStatus.OK.value());

        // Second webhook call with same eventId (idempotent)
        given()
            .spec(requestSpec)
            .body(webhookRequest)
        .when()
            .post("/pix/webhook")
        .then()
            .statusCode(HttpStatus.OK.value());

        // Balance should reflect only one credit
        given()
            .spec(requestSpec)
        .when()
            .get("/wallets/{id}/balance", destWalletId)
        .then()
            .body("balance", equalTo(150.00f));
    }

    // Helper methods
    private Integer createWallet(String userId) {
        return given()
            .spec(requestSpec)
            .body(new CreateWalletRequest(userId))
        .when()
            .post("/wallets")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .path("id");
    }

    private void registerPixKey(Integer walletId, PixKeyType keyType, String keyValue) {
        given()
            .spec(requestSpec)
            .body(new RegisterPixKeyRequest(keyType, keyValue))
        .when()
            .post("/wallets/{id}/pix-keys", walletId)
        .then()
            .statusCode(HttpStatus.CREATED.value());
    }

    private void deposit(Integer walletId, BigDecimal amount) {
        given()
            .spec(requestSpec)
            .body(new DepositRequest(amount))
        .when()
            .post("/wallets/{id}/deposit", walletId)
        .then()
            .statusCode(HttpStatus.OK.value());
    }
}

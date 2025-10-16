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
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("WalletController Integration Tests with REST Assured")
@Disabled
class WalletControllerRestAssuredTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /wallets should create wallet successfully")
    void shouldCreateWalletSuccessfully() {
        CreateWalletRequest request = new CreateWalletRequest("user123");

        given()
            .spec(requestSpec)
            .body(request)
        .when()
            .post("/wallets")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .contentType("application/json")
            .body("id", notNullValue())
            .body("userId", equalTo("user123"))
            .body("balance", equalTo(0.00f));
    }

    @Test
    @DisplayName("POST /wallets should return 400 when userId is empty")
    void shouldReturn400WhenUserIdIsEmpty() {
        CreateWalletRequest request = new CreateWalletRequest("");

        given()
            .spec(requestSpec)
            .body(request)
        .when()
            .post("/wallets")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("error", equalTo("Validation Failed"))
            .body("message", containsString("User ID is required"));
    }

    @Test
    @DisplayName("POST /wallets should return 400 when userId is null")
    void shouldReturn400WhenUserIdIsNull() {
        String requestBody = "{\"userId\": null}";

        given()
            .spec(requestSpec)
            .body(requestBody)
        .when()
            .post("/wallets")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("POST /wallets/{id}/pix-keys should register Pix key successfully")
    void shouldRegisterPixKeySuccessfully() {
        // First, create a wallet
        CreateWalletRequest walletRequest = new CreateWalletRequest("user456");

        Integer walletId = given()
            .spec(requestSpec)
            .body(walletRequest)
        .when()
            .post("/wallets")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .path("id");

        // Then, register Pix key
        RegisterPixKeyRequest pixKeyRequest = new RegisterPixKeyRequest(
            PixKeyType.EMAIL,
            "user@example.com"
        );

        given()
            .spec(requestSpec)
            .body(pixKeyRequest)
        .when()
            .post("/wallets/{id}/pix-keys", walletId)
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("id", notNullValue())
            .body("walletId", equalTo(walletId))
            .body("keyType", equalTo("EMAIL"))
            .body("keyValue", equalTo("user@example.com"));
    }

    @Test
    @DisplayName("POST /wallets/{id}/pix-keys should return 400 for invalid email")
    void shouldReturn400ForInvalidEmail() {
        // Create wallet first
        CreateWalletRequest walletRequest = new CreateWalletRequest("user789");
        Integer walletId = given()
            .spec(requestSpec)
            .body(walletRequest)
        .when()
            .post("/wallets")
        .then()
            .extract()
            .path("id");

        // Try to register invalid email
        RegisterPixKeyRequest pixKeyRequest = new RegisterPixKeyRequest(
            PixKeyType.EMAIL,
            "invalid-email"
        );

        given()
            .spec(requestSpec)
            .body(pixKeyRequest)
        .when()
            .post("/wallets/{id}/pix-keys", walletId)
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("Invalid email format"));
    }

    @Test
    @DisplayName("POST /wallets/{id}/pix-keys should return 409 for duplicate Pix key")
    void shouldReturn409ForDuplicatePixKey() {
        // Create two wallets
        Integer walletId1 = given()
            .spec(requestSpec)
            .body(new CreateWalletRequest("user1"))
        .when()
            .post("/wallets")
        .then()
            .extract()
            .path("id");

        Integer walletId2 = given()
            .spec(requestSpec)
            .body(new CreateWalletRequest("user2"))
        .when()
            .post("/wallets")
        .then()
            .extract()
            .path("id");

        // Register Pix key in first wallet
        RegisterPixKeyRequest pixKeyRequest = new RegisterPixKeyRequest(
            PixKeyType.EMAIL,
            "duplicate@example.com"
        );

        given()
            .spec(requestSpec)
            .body(pixKeyRequest)
        .when()
            .post("/wallets/{id}/pix-keys", walletId1)
        .then()
            .statusCode(HttpStatus.CREATED.value());

        // Try to register same key in second wallet
        given()
            .spec(requestSpec)
            .body(pixKeyRequest)
        .when()
            .post("/wallets/{id}/pix-keys", walletId2)
        .then()
            .statusCode(HttpStatus.CONFLICT.value())
            .body("message", equalTo("Pix key already registered"));
    }

    @Test
    @DisplayName("POST /wallets/{id}/deposit should deposit successfully")
    void shouldDepositSuccessfully() {
        // Create wallet
        Integer walletId = given()
            .spec(requestSpec)
            .body(new CreateWalletRequest("depositor"))
        .when()
            .post("/wallets")
        .then()
            .extract()
            .path("id");

        // Deposit
        DepositRequest depositRequest = new DepositRequest(new BigDecimal("100.00"));

        given()
            .spec(requestSpec)
            .body(depositRequest)
        .when()
            .post("/wallets/{id}/deposit", walletId)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(walletId))
            .body("balance", equalTo(100.00f));
    }

    @Test
    @DisplayName("POST /wallets/{id}/deposit should return 400 for negative amount")
    void shouldReturn400ForNegativeDeposit() {
        Integer walletId = given()
            .spec(requestSpec)
            .body(new CreateWalletRequest("user"))
        .when()
            .post("/wallets")
        .then()
            .extract()
            .path("id");

        DepositRequest depositRequest = new DepositRequest(new BigDecimal("-10.00"));

        given()
            .spec(requestSpec)
            .body(depositRequest)
        .when()
            .post("/wallets/{id}/deposit", walletId)
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("Amount must be greater than zero"));
    }

    @Test
    @DisplayName("POST /wallets/{id}/deposit should return 400 for zero amount")
    void shouldReturn400ForZeroDeposit() {
        Integer walletId = given()
            .spec(requestSpec)
            .body(new CreateWalletRequest("user"))
        .when()
            .post("/wallets")
        .then()
            .extract()
            .path("id");

        DepositRequest depositRequest = new DepositRequest(new BigDecimal("0.00"));

        given()
            .spec(requestSpec)
            .body(depositRequest)
        .when()
            .post("/wallets/{id}/deposit", walletId)
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("POST /wallets/{id}/withdraw should withdraw successfully")
    void shouldWithdrawSuccessfully() {
        // Create wallet and deposit
        Integer walletId = given()
            .spec(requestSpec)
            .body(new CreateWalletRequest("withdrawer"))
        .when()
            .post("/wallets")
        .then()
            .extract()
            .path("id");

        given()
            .spec(requestSpec)
            .body(new DepositRequest(new BigDecimal("200.00")))
        .when()
            .post("/wallets/{id}/deposit", walletId)
        .then()
            .statusCode(HttpStatus.OK.value());

        // Withdraw
        WithdrawRequest withdrawRequest = new WithdrawRequest(new BigDecimal("50.00"));

        given()
            .spec(requestSpec)
            .body(withdrawRequest)
        .when()
            .post("/wallets/{id}/withdraw", walletId)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("balance", equalTo(150.00f));
    }

    @Test
    @DisplayName("POST /wallets/{id}/withdraw should return 409 for insufficient balance")
    void shouldReturn409ForInsufficientBalance() {
        // Create wallet with 100
        Integer walletId = given()
            .spec(requestSpec)
            .body(new CreateWalletRequest("user"))
        .when()
            .post("/wallets")
        .then()
            .extract()
            .path("id");

        given()
            .spec(requestSpec)
            .body(new DepositRequest(new BigDecimal("100.00")))
        .when()
            .post("/wallets/{id}/deposit", walletId)
        .then()
            .statusCode(HttpStatus.OK.value());

        // Try to withdraw 150
        WithdrawRequest withdrawRequest = new WithdrawRequest(new BigDecimal("150.00"));

        given()
            .spec(requestSpec)
            .body(withdrawRequest)
        .when()
            .post("/wallets/{id}/withdraw", walletId)
        .then()
            .statusCode(HttpStatus.CONFLICT.value())
            .body("message", equalTo("Insufficient balance"));
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance should return current balance")
    void shouldReturnCurrentBalance() {
        // Create wallet and deposit
        Integer walletId = given()
            .spec(requestSpec)
            .body(new CreateWalletRequest("balanceUser"))
        .when()
            .post("/wallets")
        .then()
            .extract()
            .path("id");

        given()
            .spec(requestSpec)
            .body(new DepositRequest(new BigDecimal("250.00")))
        .when()
            .post("/wallets/{id}/deposit", walletId)
        .then()
            .statusCode(HttpStatus.OK.value());

        // Get balance
        given()
            .spec(requestSpec)
        .when()
            .get("/wallets/{id}/balance", walletId)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("walletId", equalTo(walletId))
            .body("balance", equalTo(250.00f))
            .body("timestamp", notNullValue());
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance should return 400 for non-existent wallet")
    void shouldReturn400ForNonExistentWallet() {
        given()
            .spec(requestSpec)
        .when()
            .get("/wallets/{id}/balance", 99999)
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("Wallet not found"));
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance with timestamp should return historical balance")
    void shouldReturnHistoricalBalance() {
        // Create wallet and make operations
        Integer walletId = given()
            .spec(requestSpec)
            .body(new CreateWalletRequest("historyUser"))
        .when()
            .post("/wallets")
        .then()
            .extract()
            .path("id");

        // Deposit 100
        given()
            .spec(requestSpec)
            .body(new DepositRequest(new BigDecimal("100.00")))
        .when()
            .post("/wallets/{id}/deposit", walletId)
        .then()
            .statusCode(HttpStatus.OK.value());

        // Get balance with future timestamp (should return current balance)
        String futureTimestamp = LocalDateTime.now().plusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        given()
            .spec(requestSpec)
            .queryParam("at", futureTimestamp)
        .when()
            .get("/wallets/{id}/balance", walletId)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("balance", equalTo(100.00f));
    }

    @Test
    @DisplayName("Should handle multiple operations in sequence")
    void shouldHandleMultipleOperationsInSequence() {
        // Create wallet
        Integer walletId = given()
            .spec(requestSpec)
            .body(new CreateWalletRequest("multiOpUser"))
        .when()
            .post("/wallets")
        .then()
            .extract()
            .path("id");

        // Operation 1: Deposit 500
        given()
            .spec(requestSpec)
            .body(new DepositRequest(new BigDecimal("500.00")))
        .when()
            .post("/wallets/{id}/deposit", walletId)
        .then()
            .body("balance", equalTo(500.00f));

        // Operation 2: Withdraw 150
        given()
            .spec(requestSpec)
            .body(new WithdrawRequest(new BigDecimal("150.00")))
        .when()
            .post("/wallets/{id}/withdraw", walletId)
        .then()
            .body("balance", equalTo(350.00f));

        // Operation 3: Deposit 50
        given()
            .spec(requestSpec)
            .body(new DepositRequest(new BigDecimal("50.00")))
        .when()
            .post("/wallets/{id}/deposit", walletId)
        .then()
            .body("balance", equalTo(400.00f));

        // Verify final balance
        given()
            .spec(requestSpec)
        .when()
            .get("/wallets/{id}/balance", walletId)
        .then()
            .body("balance", equalTo(400.00f));
    }
}

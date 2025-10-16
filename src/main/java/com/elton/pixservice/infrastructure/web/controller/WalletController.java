package com.elton.pixservice.infrastructure.web.controller;

import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.infrastructure.web.controller.api.WalletControllerApi;
import com.elton.pixservice.infrastructure.web.dto.*;
import com.elton.pixservice.usecase.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for Wallet management operations.
 *
 * Implements {@link WalletControllerApi} which contains all Swagger documentation.
 * This keeps the controller focused on implementation while delegating API
 * documentation to the interface.
 */
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
@Slf4j
public class WalletController implements WalletControllerApi {

    private final CreateWalletUseCase createWalletUseCase;
    private final RegisterPixKeyUseCase registerPixKeyUseCase;
    private final DepositUseCase depositUseCase;
    private final WithdrawUseCase withdrawUseCase;
    private final GetBalanceUseCase getBalanceUseCase;

    @PostMapping
    @Override
    public ResponseEntity<WalletResponse> createWallet(@RequestBody CreateWalletRequest request) {
        log.info("Received request to create wallet for user: {}", request.getUserId());

        Wallet wallet = createWalletUseCase.execute(request.getUserId());

        WalletResponse response = WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance().getAmount())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/pix-keys")
    @Override
    public ResponseEntity<PixKeyResponse> registerPixKey(
            @PathVariable Long id,
            @RequestBody RegisterPixKeyRequest request) {

        log.info("Received request to register Pix key for wallet: {}", id);

        var pixKey = registerPixKeyUseCase.execute(id, request.getKeyType(), request.getKeyValue());

        PixKeyResponse response = PixKeyResponse.builder()
                .id(pixKey.getId())
                .walletId(pixKey.getWalletId())
                .keyType(pixKey.getKeyType())
                .keyValue(pixKey.getKeyValue())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/deposit")
    @Override
    public ResponseEntity<WalletResponse> deposit(
            @PathVariable Long id,
            @RequestBody DepositRequest request) {

        log.info("Received deposit request for wallet: {}, amount: {}", id, request.getAmount());

        Wallet wallet = depositUseCase.execute(id, Money.of(request.getAmount()));

        WalletResponse response = WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance().getAmount())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/withdraw")
    @Override
    public ResponseEntity<WalletResponse> withdraw(
            @PathVariable Long id,
            @RequestBody WithdrawRequest request) {

        log.info("Received withdraw request for wallet: {}, amount: {}", id, request.getAmount());

        Wallet wallet = withdrawUseCase.execute(id, Money.of(request.getAmount()));

        WalletResponse response = WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance().getAmount())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/balance")
    @Override
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at) {

        log.info("Received balance request for wallet: {}, at: {}", id, at);

        Money balance = at != null
                ? getBalanceUseCase.getHistoricalBalance(id, at)
                : getBalanceUseCase.getCurrentBalance(id);

        BalanceResponse response = BalanceResponse.builder()
                .walletId(id)
                .balance(balance.getAmount())
                .timestamp(at != null ? at : LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }
}

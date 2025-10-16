package com.elton.pixservice.infrastructure.web.controller;

import com.elton.pixservice.domain.entity.PixTransfer;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.infrastructure.web.controller.api.PixControllerApi;
import com.elton.pixservice.infrastructure.web.dto.TransferPixRequest;
import com.elton.pixservice.infrastructure.web.dto.TransferPixResponse;
import com.elton.pixservice.infrastructure.web.dto.WebhookRequest;
import com.elton.pixservice.usecase.ProcessWebhookUseCase;
import com.elton.pixservice.usecase.TransferPixUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Pix transfer operations.
 *
 * Implements {@link PixControllerApi} which contains all Swagger documentation.
 * This keeps the controller focused on implementation while delegating API
 * documentation to the interface.
 */
@RestController
@RequestMapping("/pix")
@RequiredArgsConstructor
@Slf4j
public class PixController implements PixControllerApi {

    private final TransferPixUseCase transferPixUseCase;
    private final ProcessWebhookUseCase processWebhookUseCase;

    @PostMapping("/transfers")
    @Override
    public ResponseEntity<TransferPixResponse> transferPix(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody TransferPixRequest request) {

        log.info("Received Pix transfer request with idempotency key: {}", idempotencyKey);

        PixTransfer transfer = transferPixUseCase.execute(
                request.getFromWalletId(),
                request.getPixKey(),
                Money.of(request.getAmount()),
                idempotencyKey
        );

        TransferPixResponse response = TransferPixResponse.builder()
                .endToEndId(transfer.getEndToEndId())
                .fromWalletId(transfer.getFromWalletId())
                .toWalletId(transfer.getToWalletId())
                .amount(transfer.getAmount().getAmount())
                .status(transfer.getStatus())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/webhook")
    @Override
    public ResponseEntity<Void> processWebhook(@RequestBody WebhookRequest request) {
        log.info("Received webhook event: {}", request.getEventId());

        processWebhookUseCase.execute(
                request.getEventId(),
                request.getEndToEndId(),
                request.getEventType()
        );

        return ResponseEntity.ok().build();
    }
}

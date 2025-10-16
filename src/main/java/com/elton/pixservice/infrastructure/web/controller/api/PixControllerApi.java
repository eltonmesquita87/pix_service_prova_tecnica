package com.elton.pixservice.infrastructure.web.controller.api;

import com.elton.pixservice.infrastructure.web.dto.TransferPixRequest;
import com.elton.pixservice.infrastructure.web.dto.TransferPixResponse;
import com.elton.pixservice.infrastructure.web.dto.WebhookRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.validation.Valid;

/**
 * API interface for Pix transfer operations.
 *
 * This interface centralizes all Swagger/OpenAPI documentation for Pix endpoints,
 * keeping the implementation clean and focused on business logic.
 */
@Api(tags = "Pix Transfers", description = "Operações de transferência Pix e processamento de webhooks")
public interface PixControllerApi {

    @ApiOperation(value = "Realizar transferência Pix",
                  notes = "Cria uma transferência Pix entre carteiras. " +
                          "A transferência é criada com status PENDING e debita imediatamente da carteira origem. " +
                          "A confirmação/rejeição ocorre via webhook. " +
                          "Suporta idempotência via header Idempotency-Key.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Transferência criada com sucesso (status PENDING)"),
            @ApiResponse(code = 400, message = "Dados inválidos ou chave Pix não encontrada"),
            @ApiResponse(code = 404, message = "Carteira não encontrada"),
            @ApiResponse(code = 409, message = "Saldo insuficiente ou chave idempotência duplicada")
    })
    ResponseEntity<TransferPixResponse> transferPix(
            @ApiParam(value = "Chave de idempotência única para evitar duplicação", required = true, example = "unique-key-123")
            @RequestHeader("Idempotency-Key") String idempotencyKey,

            @ApiParam(value = "Dados da transferência Pix", required = true)
            @Valid @RequestBody TransferPixRequest request);

    @ApiOperation(value = "Processar webhook de transferência",
                  notes = "Endpoint para processar eventos de confirmação/rejeição de transferências Pix. " +
                          "Eventos suportados: CONFIRMED (credita na carteira destino) e REJECTED (estorna para carteira origem). " +
                          "Idempotente por eventId.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Webhook processado com sucesso"),
            @ApiResponse(code = 400, message = "Dados inválidos ou transição de estado inválida"),
            @ApiResponse(code = 404, message = "Transferência não encontrada")
    })
    ResponseEntity<Void> processWebhook(
            @ApiParam(value = "Dados do evento webhook", required = true)
            @Valid @RequestBody WebhookRequest request);
}

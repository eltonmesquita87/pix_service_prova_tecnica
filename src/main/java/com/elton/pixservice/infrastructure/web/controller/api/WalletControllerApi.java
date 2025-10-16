package com.elton.pixservice.infrastructure.web.controller.api;

import com.elton.pixservice.infrastructure.web.dto.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.time.LocalDateTime;

/**
 * API interface for Wallet management operations.
 *
 * This interface centralizes all Swagger/OpenAPI documentation for Wallet endpoints,
 * keeping the implementation clean and focused on business logic.
 */
@Api(tags = "Wallet Management", description = "Operações de gerenciamento de carteiras digitais")
public interface WalletControllerApi {

    @ApiOperation(value = "Criar carteira", notes = "Cria uma nova carteira digital para um usuário")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Carteira criada com sucesso"),
            @ApiResponse(code = 400, message = "Dados inválidos na requisição")
    })
    ResponseEntity<WalletResponse> createWallet(
            @ApiParam(value = "Dados da carteira a ser criada", required = true)
            @Valid @RequestBody CreateWalletRequest request);

    @ApiOperation(value = "Registrar chave Pix", notes = "Registra uma chave Pix (CPF, EMAIL, PHONE ou EVP) para uma carteira")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Chave Pix registrada com sucesso"),
            @ApiResponse(code = 400, message = "Formato de chave inválido"),
            @ApiResponse(code = 404, message = "Carteira não encontrada"),
            @ApiResponse(code = 409, message = "Chave Pix já registrada")
    })
    ResponseEntity<PixKeyResponse> registerPixKey(
            @ApiParam(value = "ID da carteira", required = true, example = "1")
            @PathVariable Long id,

            @ApiParam(value = "Dados da chave Pix", required = true)
            @Valid @RequestBody RegisterPixKeyRequest request);

    @ApiOperation(value = "Depositar", notes = "Realiza um depósito em uma carteira")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Depósito realizado com sucesso"),
            @ApiResponse(code = 400, message = "Valor inválido"),
            @ApiResponse(code = 404, message = "Carteira não encontrada")
    })
    ResponseEntity<WalletResponse> deposit(
            @ApiParam(value = "ID da carteira", required = true, example = "1")
            @PathVariable Long id,

            @ApiParam(value = "Dados do depósito", required = true)
            @Valid @RequestBody DepositRequest request);

    @ApiOperation(value = "Sacar", notes = "Realiza um saque de uma carteira")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Saque realizado com sucesso"),
            @ApiResponse(code = 400, message = "Valor inválido ou saldo insuficiente"),
            @ApiResponse(code = 404, message = "Carteira não encontrada")
    })
    ResponseEntity<WalletResponse> withdraw(
            @ApiParam(value = "ID da carteira", required = true, example = "1")
            @PathVariable Long id,

            @ApiParam(value = "Dados do saque", required = true)
            @Valid @RequestBody WithdrawRequest request);

    @ApiOperation(value = "Consultar saldo", notes = "Consulta o saldo atual ou histórico de uma carteira")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Saldo consultado com sucesso"),
            @ApiResponse(code = 404, message = "Carteira não encontrada")
    })
    ResponseEntity<BalanceResponse> getBalance(
            @ApiParam(value = "ID da carteira", required = true, example = "1")
            @PathVariable Long id,

            @ApiParam(value = "Data/hora para consulta histórica (opcional)", example = "2025-10-15T10:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at);
}

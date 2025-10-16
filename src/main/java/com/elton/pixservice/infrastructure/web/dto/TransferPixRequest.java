package com.elton.pixservice.infrastructure.web.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@ApiModel(description = "Requisição para transferência Pix")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferPixRequest {
    @ApiModelProperty(value = "ID da carteira de origem", required = true, example = "1")
    @NotNull(message = "From wallet ID is required")
    private Long fromWalletId;

    @ApiModelProperty(value = "Chave Pix do destinatário (CPF, EMAIL, PHONE ou EVP)", required = true, example = "user@example.com")
    @NotBlank(message = "Pix key is required")
    private String pixKey;

    @ApiModelProperty(value = "Valor da transferência", required = true, example = "150.00")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;
}

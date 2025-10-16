package com.elton.pixservice.infrastructure.web.dto;

import com.elton.pixservice.domain.valueobject.PixKeyType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "Requisição para registro de chave Pix")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPixKeyRequest {
    @ApiModelProperty(value = "Tipo de chave Pix", required = true, example = "EMAIL", allowableValues = "CPF, EMAIL, PHONE, EVP")
    @NotNull(message = "Key type is required")
    private PixKeyType keyType;

    @ApiModelProperty(value = "Valor da chave Pix", required = true, example = "user@example.com")
    @NotBlank(message = "Key value is required")
    private String keyValue;
}

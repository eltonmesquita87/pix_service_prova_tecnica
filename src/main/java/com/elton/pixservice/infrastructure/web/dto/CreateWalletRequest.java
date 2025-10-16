package com.elton.pixservice.infrastructure.web.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "Requisição para criação de carteira")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalletRequest {
    @ApiModelProperty(value = "ID único do usuário", required = true, example = "user123")
    @NotBlank(message = "User ID is required")
    private String userId;
}

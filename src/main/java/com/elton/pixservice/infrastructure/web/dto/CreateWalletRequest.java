package com.elton.pixservice.infrastructure.web.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ApiModel(description = "Requisição para criação de carteira")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalletRequest {
    @ApiModelProperty(value = "ID único do usuário", required = true, example = "user123")
    @NotBlank(message = "User ID is required")
    private String userId;

    @ApiModelProperty(value = "CPF do titular: 11 dígitos ou no formato ###.###.###-##, com dígitos verificadores válidos. "
            + "Opcional e único por carteira. Na resposta é devolvido mascarado", required = false, example = "123.456.789-09")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    @ToString.Exclude
    private String cpf;

    @ApiModelProperty(value = "Nome do titular: de 2 a 150 caracteres após remover espaços nas pontas. Opcional",
            required = false, example = "Maria Silva")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    @ToString.Exclude
    private String nome;

    @ApiModelProperty(value = "E-mail do titular: até 254 caracteres, gravado em minúsculas. Opcional",
            required = false, example = "maria.silva@example.com")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    @ToString.Exclude
    private String email;

    @ApiModelProperty(value = "Telefone do titular no formato E.164: '+' seguido de 8 a 15 dígitos, "
            + "o primeiro diferente de 0. Opcional", required = false, example = "+5511999999999")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    @ToString.Exclude
    private String telefone;

    public CreateWalletRequest(String userId) {
        this.userId = userId;
    }
}

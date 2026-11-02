package com.elton.pixservice.domain.exception;

/**
 * Exceção lançada quando os dados pessoais do titular da carteira são inválidos.
 * As mensagens são fixas e nunca incluem o valor recebido, pois são dados pessoais (AD-001).
 */
public class DadosTitularInvalidosException extends DomainException {

    public DadosTitularInvalidosException(String message) {
        super(message);
    }

    public static DadosTitularInvalidosException cpfInvalido() {
        return new DadosTitularInvalidosException("Invalid CPF");
    }

    public static DadosTitularInvalidosException emailInvalido() {
        return new DadosTitularInvalidosException("Invalid email");
    }

    public static DadosTitularInvalidosException telefoneInvalido() {
        return new DadosTitularInvalidosException("Invalid phone");
    }

    public static DadosTitularInvalidosException nomeInvalido() {
        return new DadosTitularInvalidosException("Invalid name");
    }
}

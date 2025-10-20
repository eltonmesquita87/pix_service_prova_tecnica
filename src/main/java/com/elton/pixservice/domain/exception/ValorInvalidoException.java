package com.elton.pixservice.domain.exception;

/**
 * Exceção lançada quando um valor monetário inválido é fornecido.
 */
public class ValorInvalidoException extends DomainException {

    public ValorInvalidoException(String message) {
        super(message);
    }

    public static ValorInvalidoException depositoDeveSerPositivo() {
        return new ValorInvalidoException("Deposit amount must be positive");
    }

    public static ValorInvalidoException saqueDeveSerPositivo() {
        return new ValorInvalidoException("Withdraw amount must be positive");
    }

    public static ValorInvalidoException valorNaoPodeSerNulo() {
        return new ValorInvalidoException("Amount cannot be null");
    }
}

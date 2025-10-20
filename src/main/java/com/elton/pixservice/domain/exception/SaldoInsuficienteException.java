package com.elton.pixservice.domain.exception;

import java.math.BigDecimal;

/**
 * Exceção lançada quando uma operação não pode ser realizada devido a saldo insuficiente.
 */
public class SaldoInsuficienteException extends DomainException {

    public SaldoInsuficienteException() {
        super("Insufficient balance");
    }

    public SaldoInsuficienteException(BigDecimal saldoAtual, BigDecimal valorSolicitado) {
        super(String.format("Insufficient balance. Current: %s, Requested: %s",
            saldoAtual, valorSolicitado));
    }

    public SaldoInsuficienteException(String message) {
        super(message);
    }
}

package com.elton.pixservice.domain.exception;

/**
 * Exceção lançada quando uma transferência Pix possui dados inválidos.
 */
public class TransferenciaInvalidaException extends DomainException {

    public TransferenciaInvalidaException(String message) {
        super(message);
    }

    public static TransferenciaInvalidaException mesmaCarteira() {
        return new TransferenciaInvalidaException("Cannot transfer to the same wallet");
    }

    public static TransferenciaInvalidaException transicaoEstadoInvalida(String estadoAtual, String novoEstado) {
        return new TransferenciaInvalidaException(
            String.format("Cannot transition from %s to %s. Invalid state transition",
                estadoAtual, novoEstado));
    }
}

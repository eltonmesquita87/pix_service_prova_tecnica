package com.elton.pixservice.domain.exception;

/**
 * Exceção lançada quando uma requisição idempotente já foi processada anteriormente.
 */
public class RequisicaoDuplicadaException extends DomainException {

    public RequisicaoDuplicadaException() {
        super("Duplicate request");
    }

    public RequisicaoDuplicadaException(String idempotencyKey) {
        super("Duplicate request for idempotency key: " + idempotencyKey);
    }
}

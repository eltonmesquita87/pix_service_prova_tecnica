package com.elton.pixservice.domain.exception;

/**
 * Exceção base para todas as exceções de domínio do Pix Service.
 * Representa violações de regras de negócio ou estados inválidos do domínio.
 */
public abstract class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

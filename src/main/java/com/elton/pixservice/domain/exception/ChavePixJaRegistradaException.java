package com.elton.pixservice.domain.exception;

/**
 * Exceção lançada quando se tenta registrar uma chave Pix que já existe no sistema.
 */
public class ChavePixJaRegistradaException extends DomainException {

    public ChavePixJaRegistradaException(String keyValue) {
        super("Pix key already registered: " + keyValue);
    }
}

package com.elton.pixservice.domain.exception;

/**
 * Exceção lançada quando um tipo de evento desconhecido é recebido.
 */
public class TipoEventoDesconhecidoException extends DomainException {

    public TipoEventoDesconhecidoException(String eventType) {
        super("Unknown event type: " + eventType);
    }
}

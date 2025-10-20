package com.elton.pixservice.domain.exception;

/**
 * Exceção lançada quando um tipo de lançamento contábil desconhecido é utilizado.
 */
public class TipoLancamentoDesconhecidoException extends DomainException {

    public TipoLancamentoDesconhecidoException(String type) {
        super("Unknown ledger entry type: " + type);
    }
}

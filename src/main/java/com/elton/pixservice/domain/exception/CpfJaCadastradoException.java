package com.elton.pixservice.domain.exception;

/**
 * Exceção lançada quando se tenta criar uma carteira com um CPF que já pertence a outra carteira.
 * A mensagem é fixa e não inclui o CPF, pois é dado pessoal (AD-001).
 */
public class CpfJaCadastradoException extends DomainException {

    public CpfJaCadastradoException() {
        super("CPF already registered");
    }
}

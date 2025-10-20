package com.elton.pixservice.domain.exception;

/**
 * Exceção lançada quando uma carteira não é encontrada no sistema.
 */
public class WalletNaoEncontradaException extends DomainException {

    public WalletNaoEncontradaException(Long walletId) {
        super("Wallet not found: " + walletId);
    }

    public WalletNaoEncontradaException(String message) {
        super(message);
    }
}

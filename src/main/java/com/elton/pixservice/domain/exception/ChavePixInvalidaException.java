package com.elton.pixservice.domain.exception;

/**
 * Exceção lançada quando uma chave Pix possui formato inválido.
 */
public class ChavePixInvalidaException extends DomainException {

    public ChavePixInvalidaException(String message) {
        super(message);
    }

    public static ChavePixInvalidaException cpfInvalido(String cpf) {
        return new ChavePixInvalidaException("Invalid CPF format: " + cpf);
    }

    public static ChavePixInvalidaException emailInvalido(String email) {
        return new ChavePixInvalidaException("Invalid email format: " + email);
    }

    public static ChavePixInvalidaException telefoneInvalido(String phone) {
        return new ChavePixInvalidaException("Invalid phone format: " + phone);
    }

    public static ChavePixInvalidaException evpInvalido(String evp) {
        return new ChavePixInvalidaException("Invalid EVP format: " + evp);
    }

    public static ChavePixInvalidaException valorVazio() {
        return new ChavePixInvalidaException("Pix key value cannot be empty");
    }

    public static ChavePixInvalidaException tipoDesconhecido(String keyType) {
        return new ChavePixInvalidaException("Unknown Pix key type: " + keyType);
    }
}

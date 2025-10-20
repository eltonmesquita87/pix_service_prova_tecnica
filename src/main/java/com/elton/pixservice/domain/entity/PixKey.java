package com.elton.pixservice.domain.entity;

import com.elton.pixservice.domain.exception.ChavePixInvalidaException;
import com.elton.pixservice.domain.valueobject.PixKeyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain entity representing a Pix key associated with a wallet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixKey {
    private Long id;
    private Long walletId;
    private PixKeyType keyType;
    private String keyValue;
    private LocalDateTime createdAt;

    public void validate() {
        if (keyValue == null || keyValue.isBlank()) {
            throw ChavePixInvalidaException.valorVazio();
        }

        switch (keyType) {
            case CPF:
                validateCpf();
                break;
            case EMAIL:
                validateEmail();
                break;
            case PHONE:
                validatePhone();
                break;
            case EVP:
                validateEvp();
                break;
            default:
                throw ChavePixInvalidaException.tipoDesconhecido(keyType.toString());
        }
    }

    private void validateCpf() {
        String cleaned = keyValue.replaceAll("[^0-9]", "");
        if (cleaned.length() != 11) {
            throw ChavePixInvalidaException.cpfInvalido(keyValue);
        }
    }

    private void validateEmail() {
        if (!keyValue.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw ChavePixInvalidaException.emailInvalido(keyValue);
        }
    }

    private void validatePhone() {
        String cleaned = keyValue.replaceAll("[^0-9]", "");
        if (cleaned.length() < 10 || cleaned.length() > 11) {
            throw ChavePixInvalidaException.telefoneInvalido(keyValue);
        }
    }

    private void validateEvp() {
        if (keyValue.length() != 36) { // UUID format
            throw ChavePixInvalidaException.evpInvalido(keyValue);
        }
    }
}

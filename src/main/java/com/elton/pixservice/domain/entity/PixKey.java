package com.elton.pixservice.domain.entity;

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
            throw new IllegalArgumentException("Pix key value cannot be empty");
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
                throw new IllegalArgumentException("Unknown Pix key type: " + keyType);
        }
    }

    private void validateCpf() {
        String cleaned = keyValue.replaceAll("[^0-9]", "");
        if (cleaned.length() != 11) {
            throw new IllegalArgumentException("Invalid CPF format");
        }
    }

    private void validateEmail() {
        if (!keyValue.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private void validatePhone() {
        String cleaned = keyValue.replaceAll("[^0-9]", "");
        if (cleaned.length() < 10 || cleaned.length() > 11) {
            throw new IllegalArgumentException("Invalid phone format");
        }
    }

    private void validateEvp() {
        if (keyValue.length() != 36) { // UUID format
            throw new IllegalArgumentException("Invalid EVP format");
        }
    }
}

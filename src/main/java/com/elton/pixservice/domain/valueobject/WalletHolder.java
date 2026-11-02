package com.elton.pixservice.domain.valueobject;

import com.elton.pixservice.domain.exception.DadosTitularInvalidosException;
import lombok.ToString;
import lombok.Value;

import java.util.Locale;

/**
 * Value Object grouping the optional personal data of a wallet holder.
 * <p>
 * A {@code null} field means "absent"; a blank string is invalid. Validation and normalization
 * happen here, in the domain, with fixed error messages that never carry the value (AD-001).
 * Name, e-mail and phone are excluded from toString; Cpf.toString is already masked.
 */
@Value
public class WalletHolder {
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_PATTERN = "^\\+[1-9]\\d{7,14}$";
    private static final int EMAIL_MAX_LENGTH = 254;
    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 150;

    Cpf cpf;
    @ToString.Exclude String nome;
    @ToString.Exclude String email;
    @ToString.Exclude String telefone;

    public static WalletHolder of(String cpf, String nome, String email, String telefone) {
        return new WalletHolder(
                cpf == null ? null : Cpf.of(cpf),
                normalizeNome(nome),
                normalizeEmail(email),
                validateTelefone(telefone));
    }

    public static WalletHolder empty() {
        return new WalletHolder(null, null, null, null);
    }

    private static String normalizeNome(String nome) {
        if (nome == null) {
            return null;
        }
        String trimmed = nome.trim();
        if (trimmed.length() < NAME_MIN_LENGTH || trimmed.length() > NAME_MAX_LENGTH) {
            throw DadosTitularInvalidosException.nomeInvalido();
        }
        return trimmed;
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        if (trimmed.length() > EMAIL_MAX_LENGTH || !trimmed.matches(EMAIL_PATTERN)) {
            throw DadosTitularInvalidosException.emailInvalido();
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static String validateTelefone(String telefone) {
        if (telefone == null) {
            return null;
        }
        if (!telefone.matches(PHONE_PATTERN)) {
            throw DadosTitularInvalidosException.telefoneInvalido();
        }
        return telefone;
    }
}

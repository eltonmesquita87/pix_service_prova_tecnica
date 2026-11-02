package com.elton.pixservice.domain.valueobject;

import com.elton.pixservice.domain.exception.DadosTitularInvalidosException;

import java.util.Objects;

/**
 * Value Object representing a valid Brazilian CPF.
 * Accepts exactly 11 digits or the ###.###.###-## mask and stores only the digits.
 * toString returns the masked value so an accidental log never exposes the full CPF.
 */
public final class Cpf {
    private static final int LENGTH = 11;
    private static final String MASKED_FORMAT = "^[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}$";
    private static final String DIGITS_FORMAT = "^[0-9]{11}$";

    private final String digits;

    private Cpf(String digits) {
        this.digits = digits;
    }

    public static Cpf of(String raw) {
        if (raw == null) {
            throw DadosTitularInvalidosException.cpfInvalido();
        }
        String digits;
        if (raw.matches(DIGITS_FORMAT)) {
            digits = raw;
        } else if (raw.matches(MASKED_FORMAT)) {
            digits = raw.replace(".", "").replace("-", "");
        } else {
            throw DadosTitularInvalidosException.cpfInvalido();
        }
        if (allDigitsEqual(digits) || !hasValidCheckDigits(digits)) {
            throw DadosTitularInvalidosException.cpfInvalido();
        }
        return new Cpf(digits);
    }

    /**
     * Rebuilds a Cpf from a value already stored in the database, without revalidating it.
     */
    public static Cpf restore(String digits) {
        return new Cpf(digits);
    }

    public String getDigits() {
        return digits;
    }

    public String masked() {
        return "***." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-**";
    }

    private static boolean allDigitsEqual(String digits) {
        for (int i = 1; i < LENGTH; i++) {
            if (digits.charAt(i) != digits.charAt(0)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasValidCheckDigits(String digits) {
        return checkDigit(digits, 9) == digits.charAt(9) - '0'
                && checkDigit(digits, 10) == digits.charAt(10) - '0';
    }

    private static int checkDigit(String digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (length + 1 - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cpf cpf = (Cpf) o;
        return digits.equals(cpf.digits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(digits);
    }

    @Override
    public String toString() {
        return masked();
    }
}

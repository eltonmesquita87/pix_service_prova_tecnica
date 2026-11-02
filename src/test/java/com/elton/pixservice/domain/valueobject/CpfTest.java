package com.elton.pixservice.domain.valueobject;

import com.elton.pixservice.domain.exception.DadosTitularInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cpf Value Object Tests")
class CpfTest {

    // 12345678909, 52998224725 and 11144477735 have correct check digits.
    // 12345678909 has remainder < 2 on the first check digit, so it covers the "digit 0" branch.
    private static final String VALID_DIGITS = "12345678909";

    @Test
    @DisplayName("WHOLD-03: of accepts 11 digits and keeps them")
    void ofAcceptsElevenDigits() {
        assertEquals("12345678909", Cpf.of("12345678909").getDigits());
    }

    @Test
    @DisplayName("WHOLD-03: of accepts the ###.###.###-## mask and stores only the 11 digits")
    void ofAcceptsMaskAndStoresOnlyDigits() {
        Cpf masked = Cpf.of("123.456.789-09");
        Cpf plain = Cpf.of("12345678909");

        assertEquals("12345678909", masked.getDigits());
        assertEquals(plain.getDigits(), masked.getDigits());
        assertEquals(plain, masked);
        assertEquals(plain.hashCode(), masked.hashCode());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"52998224725", "529.982.247-25", "11144477735", "111.444.777-35"})
    @DisplayName("WHOLD-02: of accepts other CPFs with correct check digits")
    void ofAcceptsOtherValidCpfs(String raw) {
        assertDoesNotThrow(() -> Cpf.of(raw));
    }

    @Test
    @DisplayName("WHOLD-02: mutating the first check digit makes of fail")
    void mutatedFirstCheckDigitFails() {
        for (char digit = '0'; digit <= '9'; digit++) {
            if (digit == VALID_DIGITS.charAt(9)) {
                continue;
            }
            String mutated = VALID_DIGITS.substring(0, 9) + digit + VALID_DIGITS.charAt(10);
            assertThrows(DadosTitularInvalidosException.class, () -> Cpf.of(mutated),
                    "first check digit " + digit + " must be rejected");
        }
    }

    @Test
    @DisplayName("WHOLD-02: mutating the second check digit makes of fail")
    void mutatedSecondCheckDigitFails() {
        for (char digit = '0'; digit <= '9'; digit++) {
            if (digit == VALID_DIGITS.charAt(10)) {
                continue;
            }
            String mutated = VALID_DIGITS.substring(0, 10) + digit;
            assertThrows(DadosTitularInvalidosException.class, () -> Cpf.of(mutated),
                    "second check digit " + digit + " must be rejected");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "1234567890",       // 10 digits
            "123456789091",     // 12 digits
            "1234567890a",      // letter
            "abcdefghijk",      // only letters
            "123 456 789 09",   // spaces as separator
            "123456 78909",     // space inside
            "123-456-789-09",   // other separators
            "123/456/789/09",
            "123.456.789.09",
            "123.456.78909",    // incomplete mask
            "123456.789-09",
            "123.456.789-0",    // mask with 10 digits
            "123.456.789-099",  // mask with 12 digits
            " 12345678909",     // leading space
            "12345678909 ",     // trailing space
            " 123.456.789-09 ",
            "12345678909\n",    // trailing line break
            "",                 // empty
            "   "               // blank
    })
    @NullSource
    @DisplayName("WHOLD-02: of rejects any format other than 11 digits or ###.###.###-##")
    void ofRejectsInvalidFormats(String raw) {
        DadosTitularInvalidosException ex =
                assertThrows(DadosTitularInvalidosException.class, () -> Cpf.of(raw));

        assertEquals("Invalid CPF", ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] digit {0}")
    @ValueSource(strings = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"})
    @DisplayName("WHOLD-02: of rejects 11 equal digits, 00000000000 to 99999999999")
    void ofRejectsAllEqualDigits(String digit) {
        String allEqual = digit.repeat(11);

        DadosTitularInvalidosException ex =
                assertThrows(DadosTitularInvalidosException.class, () -> Cpf.of(allEqual));

        assertEquals("Invalid CPF", ex.getMessage());
    }

    @Test
    @DisplayName("WHOLD-02: of rejects the masked form of equal digits")
    void ofRejectsMaskedAllEqualDigits() {
        assertThrows(DadosTitularInvalidosException.class, () -> Cpf.of("111.111.111-11"));
    }

    @Test
    @DisplayName("WHOLD-19: the error message does not carry the rejected value")
    void errorMessageDoesNotCarryRejectedValue() {
        DadosTitularInvalidosException ex =
                assertThrows(DadosTitularInvalidosException.class, () -> Cpf.of("12345678900"));

        assertEquals("Invalid CPF", ex.getMessage());
        assertFalse(ex.getMessage().contains("12345678900"));
    }

    @Test
    @DisplayName("WHOLD-16: masked shows only digits 4 to 9 as ***.456.789-**")
    void maskedShowsDigitsFourToNine() {
        assertEquals("***.456.789-**", Cpf.of("12345678909").masked());
        assertEquals("***.982.247-**", Cpf.of("52998224725").masked());
    }

    @Test
    @DisplayName("WHOLD-18: toString returns the masked value and not the digits")
    void toStringReturnsMaskedValue() {
        Cpf cpf = Cpf.of("12345678909");

        assertEquals("***.456.789-**", cpf.toString());
        assertFalse(cpf.toString().contains("12345678909"));
    }

    @Test
    @DisplayName("WHOLD-18: string concatenation of a Cpf never exposes the full CPF")
    void concatenationDoesNotExposeFullCpf() {
        String logged = "wallet cpf=" + Cpf.of("123.456.789-09");

        assertEquals("wallet cpf=***.456.789-**", logged);
    }

    @Test
    @DisplayName("WHOLD-03: restore does not revalidate and keeps the stored digits")
    void restoreDoesNotRevalidate() {
        Cpf restored = Cpf.restore("00000000000");

        assertEquals("00000000000", restored.getDigits());
        assertEquals("***.000.000-**", restored.masked());
    }

    @Test
    @DisplayName("WHOLD-03: restore of valid digits equals of with the same digits")
    void restoreEqualsOf() {
        assertEquals(Cpf.of("12345678909"), Cpf.restore("12345678909"));
    }
}

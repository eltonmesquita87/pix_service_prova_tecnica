package com.elton.pixservice.domain.valueobject;

import com.elton.pixservice.domain.exception.DadosTitularInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WalletHolder Value Object Tests")
class WalletHolderTest {

    private static final String CPF = "12345678909";
    private static final String NOME = "Maria Silva";
    private static final String EMAIL = "maria@example.com";
    private static final String TELEFONE = "+5511999999999";

    private static String emailWithLength(int length) {
        String domain = "@example.com";
        return "a".repeat(length - domain.length()) + domain;
    }

    private static void assertInvalid(String expectedMessage, String cpf, String nome, String email, String telefone) {
        DadosTitularInvalidosException ex = assertThrows(DadosTitularInvalidosException.class,
                () -> WalletHolder.of(cpf, nome, email, telefone));
        assertEquals(expectedMessage, ex.getMessage());
    }

    // ---------- WHOLD-01: optional fields, any subset ----------

    @Test
    @DisplayName("WHOLD-01: all-null input gives an empty holder")
    void allNullGivesEmptyHolder() {
        WalletHolder holder = WalletHolder.of(null, null, null, null);

        assertNull(holder.getCpf());
        assertNull(holder.getNome());
        assertNull(holder.getEmail());
        assertNull(holder.getTelefone());
        assertEquals(WalletHolder.empty(), holder);
    }

    @Test
    @DisplayName("WHOLD-01: empty() has no field set")
    void emptyHasNoField() {
        WalletHolder holder = WalletHolder.empty();

        assertNull(holder.getCpf());
        assertNull(holder.getNome());
        assertNull(holder.getEmail());
        assertNull(holder.getTelefone());
    }

    @Test
    @DisplayName("WHOLD-01: all four valid fields are kept")
    void allValidFieldsAreKept() {
        WalletHolder holder = WalletHolder.of(CPF, NOME, EMAIL, TELEFONE);

        assertEquals(Cpf.of(CPF), holder.getCpf());
        assertEquals(NOME, holder.getNome());
        assertEquals(EMAIL, holder.getEmail());
        assertEquals(TELEFONE, holder.getTelefone());
    }

    @Test
    @DisplayName("WHOLD-01: only cpf is kept and the other fields stay absent")
    void onlyCpfIsKept() {
        WalletHolder holder = WalletHolder.of(CPF, null, null, null);

        assertEquals("12345678909", holder.getCpf().getDigits());
        assertNull(holder.getNome());
        assertNull(holder.getEmail());
        assertNull(holder.getTelefone());
    }

    @Test
    @DisplayName("WHOLD-01: only nome is kept and the other fields stay absent")
    void onlyNomeIsKept() {
        WalletHolder holder = WalletHolder.of(null, NOME, null, null);

        assertNull(holder.getCpf());
        assertEquals(NOME, holder.getNome());
        assertNull(holder.getEmail());
        assertNull(holder.getTelefone());
    }

    @Test
    @DisplayName("WHOLD-01: only email is kept and the other fields stay absent")
    void onlyEmailIsKept() {
        WalletHolder holder = WalletHolder.of(null, null, EMAIL, null);

        assertNull(holder.getCpf());
        assertNull(holder.getNome());
        assertEquals(EMAIL, holder.getEmail());
        assertNull(holder.getTelefone());
    }

    @Test
    @DisplayName("WHOLD-01: only telefone is kept and the other fields stay absent")
    void onlyTelefoneIsKept() {
        WalletHolder holder = WalletHolder.of(null, null, null, TELEFONE);

        assertNull(holder.getCpf());
        assertNull(holder.getNome());
        assertNull(holder.getEmail());
        assertEquals(TELEFONE, holder.getTelefone());
    }

    // ---------- WHOLD-03: CPF ----------

    @Test
    @DisplayName("WHOLD-03: a masked CPF is stored as the 11 digits")
    void maskedCpfIsStoredAsDigits() {
        WalletHolder holder = WalletHolder.of("123.456.789-09", null, null, null);

        assertEquals("12345678909", holder.getCpf().getDigits());
    }

    @Test
    @DisplayName("WHOLD-02: an invalid CPF is delegated to Cpf.of and rejected with Invalid CPF")
    void invalidCpfIsRejected() {
        assertInvalid("Invalid CPF", "12345678900", NOME, EMAIL, TELEFONE);
        assertInvalid("Invalid CPF", "11111111111", null, null, null);
    }

    // ---------- WHOLD-04 / WHOLD-05: e-mail ----------

    @Test
    @DisplayName("WHOLD-05: email is trimmed and lowercased")
    void emailIsTrimmedAndLowercased() {
        WalletHolder holder = WalletHolder.of(null, null, "  Maria.Silva@Example.COM  ", null);

        assertEquals("maria.silva@example.com", holder.getEmail());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "maria",                 // no at sign
            "maria@",                // no domain
            "@example.com",          // no local part
            "maria@example",         // no top-level domain
            "maria@example.c",       // top-level domain with 1 letter
            "maria silva@example.com", // space inside
            "maria@@example.com",
            "maria@exam_ple.com"
    })
    @DisplayName("WHOLD-04: email with a bad format is rejected with Invalid email")
    void emailWithBadFormatIsRejected(String email) {
        assertInvalid("Invalid email", null, null, email, null);
    }

    @Test
    @DisplayName("WHOLD-04: email with 254 characters passes")
    void email254CharactersPasses() {
        String email = emailWithLength(254);

        assertEquals(254, email.length());
        assertEquals(email, WalletHolder.of(null, null, email, null).getEmail());
    }

    @Test
    @DisplayName("WHOLD-04: email with 255 characters is rejected with Invalid email")
    void email255CharactersIsRejected() {
        String email = emailWithLength(255);

        assertEquals(255, email.length());
        assertInvalid("Invalid email", null, null, email, null);
    }

    @Test
    @DisplayName("WHOLD-04: the 254 character limit is measured after the trim")
    void emailLimitIsMeasuredAfterTrim() {
        String email = "  " + emailWithLength(254) + "  ";

        assertEquals(emailWithLength(254), WalletHolder.of(null, null, email, null).getEmail());
    }

    // ---------- WHOLD-06: phone ----------

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"+5511999999999", "+12345678", "+123456789012345"})
    @DisplayName("WHOLD-06: phone in E.164 with 8 to 15 digits passes unchanged")
    void validPhonePassesUnchanged(String phone) {
        assertEquals(phone, WalletHolder.of(null, null, null, phone).getTelefone());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "5511999999999",     // missing plus
            "+0511999999999",    // leading zero after plus
            "+1234567",          // 7 digits
            "+1234567890123456", // 16 digits
            "+55119999abcde",    // letters
            "+55 11 99999 9999", // spaces inside
            " +5511999999999",   // leading space
            "+5511999999999 ",   // trailing space
            "++5511999999999"
    })
    @DisplayName("WHOLD-06: phone outside E.164 is rejected with Invalid phone")
    void invalidPhoneIsRejected(String phone) {
        assertInvalid("Invalid phone", null, null, null, phone);
    }

    // ---------- WHOLD-07 / WHOLD-08: name ----------

    @Test
    @DisplayName("WHOLD-08: nome is trimmed")
    void nomeIsTrimmed() {
        WalletHolder holder = WalletHolder.of(null, "  Maria Silva  ", null, null);

        assertEquals("Maria Silva", holder.getNome());
    }

    @Test
    @DisplayName("WHOLD-07: nome with 2 characters passes")
    void nome2CharactersPasses() {
        assertEquals("Jo", WalletHolder.of(null, "Jo", null, null).getNome());
    }

    @Test
    @DisplayName("WHOLD-07: nome with 150 characters passes")
    void nome150CharactersPasses() {
        String nome = "a".repeat(150);

        assertEquals(nome, WalletHolder.of(null, nome, null, null).getNome());
    }

    @Test
    @DisplayName("WHOLD-07: nome with 1 character is rejected with Invalid name")
    void nome1CharacterIsRejected() {
        assertInvalid("Invalid name", null, "J", null, null);
    }

    @Test
    @DisplayName("WHOLD-07: nome with 151 characters is rejected with Invalid name")
    void nome151CharactersIsRejected() {
        assertInvalid("Invalid name", null, "a".repeat(151), null, null);
    }

    @Test
    @DisplayName("WHOLD-07: the length limits are measured after the trim")
    void nomeLimitsAreMeasuredAfterTrim() {
        assertInvalid("Invalid name", null, "  J  ", null, null);
        assertEquals("a".repeat(150), WalletHolder.of(null, "  " + "a".repeat(150) + "  ", null, null).getNome());
    }

    // ---------- WHOLD-09: blank string is invalid, null is absent ----------

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("WHOLD-09: a blank cpf is rejected with Invalid CPF")
    void blankCpfIsRejected(String blank) {
        assertInvalid("Invalid CPF", blank, null, null, null);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("WHOLD-09: a blank nome is rejected with Invalid name")
    void blankNomeIsRejected(String blank) {
        assertInvalid("Invalid name", null, blank, null, null);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("WHOLD-09: a blank email is rejected with Invalid email")
    void blankEmailIsRejected(String blank) {
        assertInvalid("Invalid email", null, null, blank, null);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("WHOLD-09: a blank telefone is rejected with Invalid phone")
    void blankTelefoneIsRejected(String blank) {
        assertInvalid("Invalid phone", null, null, null, blank);
    }

    @Test
    @DisplayName("WHOLD-09: null in each field is absent and not an error")
    void nullIsAbsent() {
        assertDoesNotThrow(() -> WalletHolder.of(null, NOME, EMAIL, TELEFONE));
        assertDoesNotThrow(() -> WalletHolder.of(CPF, null, EMAIL, TELEFONE));
        assertDoesNotThrow(() -> WalletHolder.of(CPF, NOME, null, TELEFONE));
        assertDoesNotThrow(() -> WalletHolder.of(CPF, NOME, EMAIL, null));
    }

    // ---------- WHOLD-02/04/06/07/19: error messages carry no value ----------

    @Test
    @DisplayName("WHOLD-19: error messages never carry the rejected value")
    void errorMessagesNeverCarryRejectedValue() {
        DadosTitularInvalidosException cpf = assertThrows(DadosTitularInvalidosException.class,
                () -> WalletHolder.of("12345678900", null, null, null));
        DadosTitularInvalidosException email = assertThrows(DadosTitularInvalidosException.class,
                () -> WalletHolder.of(null, null, "not-an-email", null));
        DadosTitularInvalidosException phone = assertThrows(DadosTitularInvalidosException.class,
                () -> WalletHolder.of(null, null, null, "5511999999999"));
        DadosTitularInvalidosException nome = assertThrows(DadosTitularInvalidosException.class,
                () -> WalletHolder.of(null, "J", null, null));

        assertFalse(cpf.getMessage().contains("12345678900"));
        assertFalse(email.getMessage().contains("not-an-email"));
        assertFalse(phone.getMessage().contains("5511999999999"));
        assertEquals("Invalid name", nome.getMessage());
    }

    // ---------- WHOLD-18: toString never exposes personal data ----------

    @Test
    @DisplayName("WHOLD-18: toString contains none of the CPF digits, e-mail, phone or name")
    void toStringContainsNoPersonalData() {
        WalletHolder holder = WalletHolder.of(CPF, NOME, EMAIL, TELEFONE);

        String text = holder.toString();

        assertFalse(text.contains("12345678909"), text);
        assertFalse(text.contains("123.456.789-09"), text);
        assertFalse(text.contains("Maria"), text);
        assertFalse(text.contains("maria@example.com"), text);
        assertFalse(text.contains("5511999999999"), text);
        assertTrue(text.contains("***.456.789-**"), text);
    }
}

package com.elton.pixservice.domain.entity;

import com.elton.pixservice.domain.valueobject.PixKeyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PixKey Entity Tests")
class PixKeyTest {

    @Test
    @DisplayName("Should validate valid CPF")
    void shouldValidateValidCpf() {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.CPF)
                .keyValue("12345678901")
                .build();

        // When & Then
        assertDoesNotThrow(pixKey::validate);
    }

    @Test
    @DisplayName("Should validate CPF with formatting")
    void shouldValidateCpfWithFormatting() {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.CPF)
                .keyValue("123.456.789-01")
                .build();

        // When & Then
        assertDoesNotThrow(pixKey::validate);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12345", "123456789012345"})
    @DisplayName("Should throw exception for invalid CPF length")
    void shouldThrowExceptionForInvalidCpfLength(String invalidCpf) {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.CPF)
                .keyValue(invalidCpf)
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, pixKey::validate);
    }

    @Test
    @DisplayName("Should validate valid email")
    void shouldValidateValidEmail() {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.EMAIL)
                .keyValue("user@example.com")
                .build();

        // When & Then
        assertDoesNotThrow(pixKey::validate);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "user.name@example.com",
        "user+tag@example.co.uk",
        "user_name@subdomain.example.com"
    })
    @DisplayName("Should validate various valid email formats")
    void shouldValidateVariousValidEmailFormats(String email) {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.EMAIL)
                .keyValue(email)
                .build();

        // When & Then
        assertDoesNotThrow(pixKey::validate);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "user@", "@example.com", "user @example.com", "user@.com"})
    @DisplayName("Should throw exception for invalid email")
    void shouldThrowExceptionForInvalidEmail(String invalidEmail) {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.EMAIL)
                .keyValue(invalidEmail)
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, pixKey::validate);
    }

    @Test
    @DisplayName("Should validate valid phone")
    void shouldValidateValidPhone() {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.PHONE)
                .keyValue("11987654321")
                .build();

        // When & Then
        assertDoesNotThrow(pixKey::validate);
    }

    @Test
    @DisplayName("Should validate phone with formatting")
    void shouldValidatePhoneWithFormatting() {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.PHONE)
                .keyValue("(11) 98765-4321")
                .build();

        // When & Then
        assertDoesNotThrow(pixKey::validate);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12345", "123456789012345"})
    @DisplayName("Should throw exception for invalid phone length")
    void shouldThrowExceptionForInvalidPhoneLength(String invalidPhone) {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.PHONE)
                .keyValue(invalidPhone)
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, pixKey::validate);
    }

    @Test
    @DisplayName("Should validate valid EVP (UUID)")
    void shouldValidateValidEvp() {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.EVP)
                .keyValue("123e4567-e89b-12d3-a456-426614174000")
                .build();

        // When & Then
        assertDoesNotThrow(pixKey::validate);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "123", "123e4567-e89b-12d3-a456"})
    @DisplayName("Should throw exception for invalid EVP")
    void shouldThrowExceptionForInvalidEvp(String invalidEvp) {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.EVP)
                .keyValue(invalidEvp)
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, pixKey::validate);
    }

    @Test
    @DisplayName("Should throw exception for empty key value")
    void shouldThrowExceptionForEmptyKeyValue() {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.EMAIL)
                .keyValue("")
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, pixKey::validate);
    }

    @Test
    @DisplayName("Should throw exception for blank key value")
    void shouldThrowExceptionForBlankKeyValue() {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.EMAIL)
                .keyValue("   ")
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, pixKey::validate);
    }

    @Test
    @DisplayName("Should throw exception for null key value")
    void shouldThrowExceptionForNullKeyValue() {
        // Given
        PixKey pixKey = PixKey.builder()
                .keyType(PixKeyType.EMAIL)
                .keyValue(null)
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, pixKey::validate);
    }
}

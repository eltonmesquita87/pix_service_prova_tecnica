package com.elton.pixservice.domain.valueobject;

import com.elton.pixservice.domain.exception.ValorInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Money Value Object Tests")
class MoneyTest {

    @Test
    @DisplayName("Should create Money from BigDecimal")
    void shouldCreateMoneyFromBigDecimal() {
        // Given
        BigDecimal amount = new BigDecimal("100.50");

        // When
        Money money = Money.of(amount);

        // Then
        assertNotNull(money);
        assertEquals(new BigDecimal("100.50"), money.getAmount());
    }

    @Test
    @DisplayName("Should create Money from double")
    void shouldCreateMoneyFromDouble() {
        // Given
        double amount = 100.50;

        // When
        Money money = Money.of(amount);

        // Then
        assertNotNull(money);
        assertEquals(0, new BigDecimal("100.50").compareTo(money.getAmount()));
    }

    @Test
    @DisplayName("Should create Money from long")
    void shouldCreateMoneyFromLong() {
        // Given
        long amount = 100L;

        // When
        Money money = Money.of(amount);

        // Then
        assertNotNull(money);
        assertEquals(new BigDecimal("100.00"), money.getAmount());
    }

    @Test
    @DisplayName("Should create zero Money")
    void shouldCreateZeroMoney() {
        // When
        Money money = Money.zero();

        // Then
        assertNotNull(money);
        assertEquals(BigDecimal.ZERO.setScale(2), money.getAmount());
        assertTrue(money.isZero());
    }

    @Test
    @DisplayName("Should throw exception when creating Money with null amount")
    void shouldThrowExceptionWhenCreatingMoneyWithNullAmount() {
        // When & Then
        assertThrows(ValorInvalidoException.class, () -> Money.of((BigDecimal) null));
    }

    @Test
    @DisplayName("Should add two Money values")
    void shouldAddTwoMoneyValues() {
        // Given
        Money money1 = Money.of(100.00);
        Money money2 = Money.of(50.50);

        // When
        Money result = money1.add(money2);

        // Then
        assertEquals(new BigDecimal("150.50"), result.getAmount());
    }

    @Test
    @DisplayName("Should subtract two Money values")
    void shouldSubtractTwoMoneyValues() {
        // Given
        Money money1 = Money.of(100.00);
        Money money2 = Money.of(30.50);

        // When
        Money result = money1.subtract(money2);

        // Then
        assertEquals(new BigDecimal("69.50"), result.getAmount());
    }

    @ParameterizedTest
    @CsvSource({
        "100.00, true",
        "0.00, false",
        "-10.00, false"
    })
    @DisplayName("Should check if Money is positive")
    void shouldCheckIfMoneyIsPositive(double amount, boolean expected) {
        // Given
        Money money = Money.of(amount);

        // When & Then
        assertEquals(expected, money.isPositive());
    }

    @ParameterizedTest
    @CsvSource({
        "-100.00, true",
        "0.00, false",
        "10.00, false"
    })
    @DisplayName("Should check if Money is negative")
    void shouldCheckIfMoneyIsNegative(double amount, boolean expected) {
        // Given
        Money money = Money.of(amount);

        // When & Then
        assertEquals(expected, money.isNegative());
    }

    @Test
    @DisplayName("Should check if Money is zero")
    void shouldCheckIfMoneyIsZero() {
        // Given
        Money zero = Money.zero();
        Money nonZero = Money.of(10.00);

        // When & Then
        assertTrue(zero.isZero());
        assertFalse(nonZero.isZero());
    }

    @Test
    @DisplayName("Should compare Money values - greater than")
    void shouldCompareMoneyValuesGreaterThan() {
        // Given
        Money money1 = Money.of(100.00);
        Money money2 = Money.of(50.00);

        // When & Then
        assertTrue(money1.isGreaterThan(money2));
        assertFalse(money2.isGreaterThan(money1));
    }

    @Test
    @DisplayName("Should compare Money values - greater than or equal")
    void shouldCompareMoneyValuesGreaterThanOrEqual() {
        // Given
        Money money1 = Money.of(100.00);
        Money money2 = Money.of(100.00);
        Money money3 = Money.of(50.00);

        // When & Then
        assertTrue(money1.isGreaterThanOrEqual(money2));
        assertTrue(money1.isGreaterThanOrEqual(money3));
        assertFalse(money3.isGreaterThanOrEqual(money1));
    }

    @Test
    @DisplayName("Should compare Money values - less than")
    void shouldCompareMoneyValuesLessThan() {
        // Given
        Money money1 = Money.of(50.00);
        Money money2 = Money.of(100.00);

        // When & Then
        assertTrue(money1.isLessThan(money2));
        assertFalse(money2.isLessThan(money1));
    }

    @Test
    @DisplayName("Should have value equality")
    void shouldHaveValueEquality() {
        // Given
        Money money1 = Money.of(100.00);
        Money money2 = Money.of(100.00);
        Money money3 = Money.of(50.00);

        // When & Then
        assertEquals(money1, money2);
        assertNotEquals(money1, money3);
    }

    @Test
    @DisplayName("Should have consistent hashCode")
    void shouldHaveConsistentHashCode() {
        // Given
        Money money1 = Money.of(100.00);
        Money money2 = Money.of(100.00);

        // When & Then
        assertEquals(money1.hashCode(), money2.hashCode());
    }

    @Test
    @DisplayName("Should format to string")
    void shouldFormatToString() {
        // Given
        Money money = Money.of(100.50);

        // When
        String result = money.toString();

        // Then
        assertEquals("100.50", result);
    }

    @Test
    @DisplayName("Should round to 2 decimal places")
    void shouldRoundToTwoDecimalPlaces() {
        // Given
        Money money = Money.of(new BigDecimal("100.999"));

        // When & Then
        assertEquals(new BigDecimal("101.00"), money.getAmount());
    }

    @Test
    @DisplayName("Money should be immutable")
    void moneyShouldBeImmutable() {
        // Given
        Money original = Money.of(100.00);

        // When
        Money added = original.add(Money.of(50.00));
        Money subtracted = original.subtract(Money.of(30.00));

        // Then
        assertEquals(new BigDecimal("100.00"), original.getAmount());
        assertEquals(new BigDecimal("150.00"), added.getAmount());
        assertEquals(new BigDecimal("70.00"), subtracted.getAmount());
    }
}

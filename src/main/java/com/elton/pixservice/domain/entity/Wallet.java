package com.elton.pixservice.domain.entity;

import com.elton.pixservice.domain.valueobject.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain entity representing a digital wallet.
 * Contains business logic for wallet operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {
    private Long id;
    private String userId;
    private Money balance;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void deposit(Money amount) {
        if (amount.isNegative() || amount.isZero()) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

    public void withdraw(Money amount) {
        if (amount.isNegative() || amount.isZero()) {
            throw new IllegalArgumentException("Withdraw amount must be positive");
        }
        if (this.balance.isLessThan(amount)) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
    }

    public boolean hasSufficientBalance(Money amount) {
        return this.balance.isGreaterThanOrEqual(amount);
    }
}

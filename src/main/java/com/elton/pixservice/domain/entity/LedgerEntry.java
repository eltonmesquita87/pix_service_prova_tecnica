package com.elton.pixservice.domain.entity;

import com.elton.pixservice.domain.valueobject.LedgerEntryType;
import com.elton.pixservice.domain.valueobject.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain entity representing an immutable ledger entry.
 * Records all financial operations for audit purposes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {
    private Long id;
    private Long walletId;
    private Money amount;
    private LedgerEntryType type;
    private String endToEndId;
    private String metadata;
    private LocalDateTime createdAt;

    /**
     * Returns the signed amount for balance calculation.
     * Debits are negative, credits are positive.
     */
    public Money getSignedAmount() {
        switch (type) {
            case DEPOSIT:
            case TRANSFER_CREDIT:
                return amount;
            case WITHDRAW:
            case TRANSFER_DEBIT:
                return Money.of(amount.getAmount().negate());
            default:
                throw new IllegalStateException("Unknown ledger entry type: " + type);
        }
    }
}

package com.elton.pixservice.domain.valueobject;

/**
 * Status lifecycle of a Pix transfer.
 * PENDING -> CONFIRMED (success)
 * PENDING -> REJECTED (failure)
 */
public enum TransferStatus {
    PENDING,
    CONFIRMED,
    REJECTED;

    public boolean canTransitionTo(TransferStatus newStatus) {
        switch (this) {
            case PENDING:
                return newStatus == CONFIRMED || newStatus == REJECTED;
            case CONFIRMED:
            case REJECTED:
                return false; // Terminal states
            default:
                return false;
        }
    }
}

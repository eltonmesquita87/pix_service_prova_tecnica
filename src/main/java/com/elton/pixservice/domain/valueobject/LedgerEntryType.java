package com.elton.pixservice.domain.valueobject;

/**
 * Types of ledger entries for the immutable audit log.
 */
public enum LedgerEntryType {
    DEPOSIT,           // Depósito na carteira
    WITHDRAW,          // Saque da carteira
    TRANSFER_DEBIT,    // Débito de transferência Pix (origem)
    TRANSFER_CREDIT    // Crédito de transferência Pix (destino)
}

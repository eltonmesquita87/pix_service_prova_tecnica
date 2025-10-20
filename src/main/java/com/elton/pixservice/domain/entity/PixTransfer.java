package com.elton.pixservice.domain.entity;

import com.elton.pixservice.domain.exception.TransferenciaInvalidaException;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.domain.valueobject.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain entity representing a Pix transfer between wallets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixTransfer {
    private String endToEndId;
    private Long fromWalletId;
    private Long toWalletId;
    private Money amount;
    private TransferStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime rejectedAt;

    public void confirm() {
        if (!status.canTransitionTo(TransferStatus.CONFIRMED)) {
            throw TransferenciaInvalidaException.transicaoEstadoInvalida(
                status.toString(), TransferStatus.CONFIRMED.toString()
            );
        }
        this.status = TransferStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void reject() {
        if (!status.canTransitionTo(TransferStatus.REJECTED)) {
            throw TransferenciaInvalidaException.transicaoEstadoInvalida(
                status.toString(), TransferStatus.REJECTED.toString()
            );
        }
        this.status = TransferStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return status == TransferStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == TransferStatus.CONFIRMED;
    }

    public boolean isRejected() {
        return status == TransferStatus.REJECTED;
    }
}

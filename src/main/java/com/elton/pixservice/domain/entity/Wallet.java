package com.elton.pixservice.domain.entity;

import com.elton.pixservice.domain.exception.SaldoInsuficienteException;
import com.elton.pixservice.domain.exception.ValorInvalidoException;
import com.elton.pixservice.domain.valueobject.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade de domínio representando uma carteira digital.
 * Contém a lógica de negócio para operações de carteira.
 *
 * @author Sistema Pix
 * @version 1.0
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
            throw ValorInvalidoException.depositoDeveSerPositivo();
        }
        this.balance = this.balance.add(amount);
    }

    public void withdraw(Money amount) {
        if (amount.isNegative() || amount.isZero()) {
            throw ValorInvalidoException.saqueDeveSerPositivo();
        }
        if (this.balance.isLessThan(amount)) {
            throw new SaldoInsuficienteException(this.balance.getAmount(), amount.getAmount());
        }
        this.balance = this.balance.subtract(amount);
    }

    public boolean hasSufficientBalance(Money amount) {
        return this.balance.isGreaterThanOrEqual(amount);
    }
}

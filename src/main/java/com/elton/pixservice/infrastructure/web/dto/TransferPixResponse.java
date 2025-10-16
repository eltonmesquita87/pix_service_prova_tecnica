package com.elton.pixservice.infrastructure.web.dto;

import com.elton.pixservice.domain.valueobject.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferPixResponse {
    private String endToEndId;
    private Long fromWalletId;
    private Long toWalletId;
    private BigDecimal amount;
    private TransferStatus status;
}

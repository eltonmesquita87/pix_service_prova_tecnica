package com.elton.pixservice.infrastructure.web.dto;

import com.elton.pixservice.domain.valueobject.PixKeyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixKeyResponse {
    private Long id;
    private Long walletId;
    private PixKeyType keyType;
    private String keyValue;
}

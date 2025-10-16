package com.elton.pixservice.usecase;

import com.elton.pixservice.domain.entity.PixKey;
import com.elton.pixservice.domain.repository.PixKeyRepository;
import com.elton.pixservice.domain.repository.WalletRepository;
import com.elton.pixservice.domain.valueobject.PixKeyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterPixKeyUseCase {

    private final PixKeyRepository pixKeyRepository;
    private final WalletRepository walletRepository;

    @Transactional
    public PixKey execute(Long walletId, PixKeyType keyType, String keyValue) {
        log.info("Registering Pix key for wallet: {}, type: {}, value: {}", walletId, keyType, keyValue);

        // Validate wallet exists
        if (!walletRepository.existsById(walletId)) {
            throw new IllegalArgumentException("Wallet not found: " + walletId);
        }

        // Check if key already exists
        if (pixKeyRepository.existsByKeyValue(keyValue)) {
            throw new IllegalStateException("Pix key already registered: " + keyValue);
        }

        PixKey pixKey = PixKey.builder()
                .walletId(walletId)
                .keyType(keyType)
                .keyValue(keyValue)
                .build();

        // Validate key format
        pixKey.validate();

        PixKey savedKey = pixKeyRepository.save(pixKey);
        log.info("Pix key registered successfully with id: {}", savedKey.getId());

        return savedKey;
    }
}

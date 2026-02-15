package com.space.space_bundle.out.persistence.mapper;

import com.space.space_bundle.core.entities.Wallet;
import com.space.space_bundle.out.persistence.entity.WalletDocument;
import lombok.experimental.UtilityClass;

@UtilityClass
public class WalletMapper {

    public static WalletDocument toDocument(Wallet wallet) {
        return WalletDocument.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().name())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    public static Wallet toDomain(WalletDocument doc) {
        return Wallet.builder()
                .id(doc.getId())
                .userId(doc.getUserId())
                .balance(doc.getBalance())
                .currency(doc.getCurrency())
                .status(Wallet.WalletStatus.valueOf(doc.getStatus()))
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
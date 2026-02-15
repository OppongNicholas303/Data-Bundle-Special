package com.space.space_bundle.core.port.out;

public interface ProviderPort {

    ProviderResult purchaseBundle(
            String network,
            String phoneNumber,
            String bundleCode
    );

    record ProviderResult(
            boolean success,
            String reference,
            String failureReason
    ) {}
}

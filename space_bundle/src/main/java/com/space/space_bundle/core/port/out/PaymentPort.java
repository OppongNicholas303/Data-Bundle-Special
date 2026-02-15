package com.space.space_bundle.core.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentPort {
    String debit(UUID userId, BigDecimal amount);
    String refund(UUID userId, BigDecimal amount);
}
package com.space.space_bundle.core.port.out;

import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.core.port.out.dto.PackageDto;
import com.space.space_bundle.core.port.out.dto.PackageResponseDto;
import com.space.space_bundle.out.automation.dto.BotPurchaseResponse;

import java.util.List;

public interface AutomationPort {
    String buyDataBundle(Order order);
    BotPurchaseResponse checkOrderStatus(String orderId);
    List<PackageDto> getBundlePackage();

}
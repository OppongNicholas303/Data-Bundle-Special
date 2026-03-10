package com.space.space_bundle.core.port.out;

import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.out.automation.dto.BotPurchaseResponseRandy;

public interface AutomationPort {
    String buyDataBundle(Order order);
    String buyDataBundleFromRandy(Order order);
    BotPurchaseResponseRandy checkOrderStatusFromRandy(String orderNumber);

}
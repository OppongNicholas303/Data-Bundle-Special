package com.space.space_bundle.core.port.out;

import com.space.space_bundle.core.entities.Order;

public interface AutomationPort {
    String buyDataBundle(Order order);
}
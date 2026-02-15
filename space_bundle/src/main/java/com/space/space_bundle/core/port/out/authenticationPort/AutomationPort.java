package com.space.space_bundle.core.port.out.authenticationPort;


import com.space.space_bundle.core.entities.Order;

public interface AutomationPort {

    /**
     * Performs the data bundle purchase for the given order.
     * Returns a provider reference or throws an exception if failed.
     */
    String buyDataBundle(Order order) throws Exception;
}


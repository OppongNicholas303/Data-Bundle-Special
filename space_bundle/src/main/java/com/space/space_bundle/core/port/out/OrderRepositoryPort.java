package com.space.space_bundle.core.port.out;

import com.space.space_bundle.core.entities.Order;
import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {
    Order save(Order order);

    Optional<Order> findById(String id);

    List<Order> findByFilters(String userId, String orderId, String phoneNumber, String status);

    Optional<Order> findLatestByPhoneNumber(String phoneNumber);
}
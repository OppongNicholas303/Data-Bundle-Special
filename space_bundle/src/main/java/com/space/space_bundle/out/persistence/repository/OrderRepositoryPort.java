package com.space.space_bundle.out.persistence.repository;

import com.space.space_bundle.core.entities.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(String orderId);

    List<Order> findByUserId(String userId);
}

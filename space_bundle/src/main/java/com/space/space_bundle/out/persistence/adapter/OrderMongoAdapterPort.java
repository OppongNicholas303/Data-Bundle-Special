package com.space.space_bundle.out.persistence.adapter;

import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.core.port.out.OrderRepositoryPort;
import com.space.space_bundle.out.persistence.entity.OrderDocument;
import com.space.space_bundle.out.persistence.mapper.OrderMapper;
import com.space.space_bundle.out.persistence.repository.SpringOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderMongoAdapterPort implements OrderRepositoryPort {

    private final SpringOrderRepository repository;

    @Override
    public Order save(Order order) {
        OrderDocument doc = OrderMapper.toDocument(order);
        doc = repository.save(doc);
        return OrderMapper.toDomain(doc);
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return repository.findById(orderId)
                .map(OrderMapper::toDomain);
    }

    @Override
    public List<Order> findByFilters(String userId, String orderId, String phoneNumber, String status) {
        List<OrderDocument> documents;
        
        if (orderId != null) {
            documents = repository.findById(orderId)
                    .filter(doc -> doc.getUserId().equals(userId))
                    .map(List::of)
                    .orElse(List.of());
        } else if (phoneNumber != null && status != null) {
            documents = repository.findByUserIdAndPhoneNumberAndStatus(userId, phoneNumber, status);
        } else if (phoneNumber != null) {
            documents = repository.findByUserIdAndPhoneNumber(userId, phoneNumber);
        } else if (status != null) {
            documents = repository.findByUserIdAndStatus(userId, status);
        } else {
            documents = repository.findByUserId(userId);
        }
        
        return documents.stream()
                .map(OrderMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByPhoneNumber(String phoneNumber) {
        return repository.findByPhoneNumber(phoneNumber).stream()
                .map(OrderMapper::toDomain)
                .collect(Collectors.toList());
    }
}

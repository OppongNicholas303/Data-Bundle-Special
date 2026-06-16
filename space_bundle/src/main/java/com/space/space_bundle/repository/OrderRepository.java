package com.space.space_bundle.repository;

import com.space.space_bundle.entity.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUserId(String userId);
    List<Order> findByAgentId(String agentId);
    List<Order> findByAgentIdAndCreatedAtBetween(String agentId, LocalDateTime from, LocalDateTime to);
    List<Order> findByAgentIdAndStatus(String agentId, String status);
    List<Order> findByAgentIdAndStatusAndCreatedAtBetween(String agentId, String status, LocalDateTime from, LocalDateTime to);
    List<Order> findByUserIdAndStatus(String userId, String status);
    List<Order> findByUserIdAndPhoneNumber(String userId, String phoneNumber);
    List<Order> findByUserIdAndPhoneNumberAndStatus(String userId, String phoneNumber, String status);
    Optional<Order> findFirstByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);
    List<Order> findByStatus(String status);
    List<Order> findByNetwork(String network);
    List<Order> findByStatusAndNetwork(String status, String network);
    List<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    List<Order> findByStatusAndCreatedAtBetween(String status, LocalDateTime from, LocalDateTime to);
    List<Order> findByNetworkAndCreatedAtBetween(String network, LocalDateTime from, LocalDateTime to);
    List<Order> findByStatusAndNetworkAndCreatedAtBetween(String status, String network, LocalDateTime from, LocalDateTime to);
    @Query(value = "{ 'status': 'COMPLETED' }", fields = "{ 'phoneNumber': 1 }")
    List<Order> findCompletedOrders();

}

package com.space.space_bundle.service;

import com.space.space_bundle.entity.Bundle;
import com.space.space_bundle.entity.Order;
import com.space.space_bundle.repository.BundleRepository;
import com.space.space_bundle.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMigrationService {

    private final OrderRepository orderRepository;
    private final BundleRepository bundleRepository;

    /**
     * Backfills costPrice and baseAmount on all orders where either field is null.
     * Safe to run multiple times — skips orders that already have both fields set.
     * Returns a summary map with patched/skipped/failed counts.
     */
    public java.util.Map<String, Object> backfillCostPrices() {
        List<Order> all = orderRepository.findAll();
        AtomicInteger patched = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        AtomicInteger failed  = new AtomicInteger();

        for (Order order : all) {
            // Skip if both fields already set
            if (order.getCostPrice() != null && order.getBaseAmount() != null) {
                skipped.incrementAndGet();
                continue;
            }

            if (order.getBundleCode() == null || order.getNetwork() == null) {
                log.warn("[MIGRATION] Order {} missing bundleCode or network — skipping", order.getId());
                failed.incrementAndGet();
                continue;
            }

            Optional<Bundle> bundleOpt = bundleRepository
                    .findByCodeAndNetwork(order.getBundleCode(), order.getNetwork().toLowerCase());

            if (bundleOpt.isEmpty()) {
                // Try uppercase network fallback
                bundleOpt = bundleRepository
                        .findByCodeAndNetwork(order.getBundleCode(), order.getNetwork().toUpperCase());
            }

            if (bundleOpt.isEmpty()) {
                log.warn("[MIGRATION] Bundle not found for order {} — code={}, network={}",
                        order.getId(), order.getBundleCode(), order.getNetwork());
                failed.incrementAndGet();
                continue;
            }

            Bundle bundle = bundleOpt.get();
            boolean changed = false;

            if (order.getCostPrice() == null && bundle.getCostPrice() != null) {
                order.setCostPrice(bundle.getCostPrice());
                changed = true;
            }

            if (order.getBaseAmount() == null && bundle.getSellingPrice() != null) {
                order.setBaseAmount(bundle.getSellingPrice());
                changed = true;
            }

            // Also default commissionAmount to ZERO if null (direct orders)
            if (order.getCommissionAmount() == null) {
                order.setCommissionAmount(BigDecimal.ZERO);
                changed = true;
            }

            if (changed) {
                orderRepository.save(order);
                patched.incrementAndGet();
                log.info("[MIGRATION] Patched order {} — cost={}, base={}",
                        order.getId(), order.getCostPrice(), order.getBaseAmount());
            } else {
                skipped.incrementAndGet();
            }
        }

        log.info("[MIGRATION] Complete — patched={}, skipped={}, failed={}",
                patched.get(), skipped.get(), failed.get());

        return java.util.Map.of(
                "total",   all.size(),
                "patched", patched.get(),
                "skipped", skipped.get(),
                "failed",  failed.get()
        );
    }
}

package com.space.space_bundle.controller;

import com.space.space_bundle.dto.AdminOrderView;
import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.service.OrderMigrationService;
import com.space.space_bundle.dto.CreateBundleRequest;
import com.space.space_bundle.entity.*;
import com.space.space_bundle.repository.*;
import com.space.space_bundle.service.AdminWithdrawalService;
import com.space.space_bundle.service.BundleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final BundleService bundleService;
    private final OrderRepository orderRepository;
    private final CommissionRepository commissionRepository;
    private final AdminWithdrawalService adminWithdrawalService;
    private final OrderMigrationService orderMigrationService;

    // ── Users ──────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userRepository.findAll()));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/users/{id}/toggle-lock")
    public ResponseEntity<ApiResponse<User>> toggleUserLock(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setAccountNonLocked(!user.isAccountNonLocked());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(
                user.isAccountNonLocked() ? "User unlocked" : "User locked", user));
    }

    @PutMapping("/users/{id}/toggle-enabled")
    public ResponseEntity<ApiResponse<User>> toggleUserEnabled(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(
                user.isEnabled() ? "User enabled" : "User disabled", user));
    }

    @PutMapping("/users/{id}/roles")
    public ResponseEntity<ApiResponse<User>> updateUserRoles(
            @PathVariable String id, @RequestBody UpdateRolesRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setRoles(request.getRoles());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Roles updated", user));
    }

    // ── Agents ─────────────────────────────────────────────────────────────

    @GetMapping("/agents")
    public ResponseEntity<ApiResponse<List<AgentProfile>>> getAllAgents() {
        return ResponseEntity.ok(ApiResponse.success(agentProfileRepository.findAll()));
    }

    @GetMapping("/agents/{id}")
    public ResponseEntity<ApiResponse<AgentProfile>> getAgentById(@PathVariable String id) {
        AgentProfile agent = agentProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(agent));
    }

    @PutMapping("/agents/{id}/toggle-active")
    public ResponseEntity<ApiResponse<AgentProfile>> toggleAgentActive(@PathVariable String id) {
        AgentProfile agent = agentProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));
        agent.setActive(!agent.isActive());
        agentProfileRepository.save(agent);
        return ResponseEntity.ok(ApiResponse.success(
                agent.isActive() ? "Agent activated" : "Agent deactivated", agent));
    }

    @GetMapping("/agents/{id}/commissions")
    public ResponseEntity<ApiResponse<List<Commission>>> getAgentCommissions(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(commissionRepository.findByAgentId(id)));
    }

    @GetMapping("/agents/{id}/orders")
    public ResponseEntity<ApiResponse<List<AdminOrderView>>> getAgentOrders(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(
                orderRepository.findByAgentId(id).stream().map(AdminOrderView::from).toList()));
    }

    @GetMapping("/agents/{id}/withdrawals")
    public ResponseEntity<ApiResponse<List<WithdrawalRequest>>> getAgentWithdrawals(@PathVariable String id) {
        // Here id is AgentProfile.id, but withdrawal uses agentUserId or agentProfileId.
        // Wait, withdrawalRequest has agentProfileId, so we can use that if we add a method to WithdrawalRepository.
        // Let's check WithdrawalRepository. We can add findByAgentProfileIdOrderByCreatedAtDesc
        return ResponseEntity.ok(ApiResponse.success(
            adminWithdrawalService.getByAgentProfileId(id)
        ));
    }

    // ── Withdrawals ────────────────────────────────────────────────────────

    @GetMapping("/withdrawals")
    public ResponseEntity<ApiResponse<List<WithdrawalRequest>>> getAllWithdrawals(
            @RequestParam(required = false) String status) {
        List<WithdrawalRequest> list = status != null
                ? adminWithdrawalService.getByStatus(status)
                : adminWithdrawalService.getAll();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/withdrawals/{id}/approve")
    public ResponseEntity<ApiResponse<WithdrawalRequest>> approveWithdrawal(
            @PathVariable String id,
            @RequestBody(required = false) AdminNoteRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Withdrawal approved and sent",
                adminWithdrawalService.approve(id, body != null ? body.getNote() : null)));
    }

    @PostMapping("/withdrawals/{id}/reject")
    public ResponseEntity<ApiResponse<WithdrawalRequest>> rejectWithdrawal(
            @PathVariable String id,
            @RequestBody(required = false) AdminNoteRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Withdrawal rejected and refunded",
                adminWithdrawalService.reject(id, body != null ? body.getNote() : null)));
    }

    // ── Bundles ────────────────────────────────────────────────────────────

    @GetMapping("/bundles")
    public ResponseEntity<ApiResponse<List<Bundle>>> getAllBundles() {
        return ResponseEntity.ok(ApiResponse.success(bundleService.getAll()));
    }

    @PostMapping("/bundles")
    public ResponseEntity<ApiResponse<Bundle>> createBundle(@RequestBody CreateBundleRequest req) {
        Bundle bundle = bundleService.create(req.getCode(), req.getName(), req.getDataSize(),
                req.getNetwork(), req.getCostPrice(), req.getSellingPrice(), req.getDescription());
        return ResponseEntity.ok(ApiResponse.success("Bundle created", bundle));
    }

    @PutMapping("/bundles/{id}")
    public ResponseEntity<ApiResponse<Bundle>> updateBundle(
            @PathVariable String id, @RequestBody UpdateBundleRequest req) {
        Bundle bundle = bundleService.update(id, req.getName(), req.getDataSize(),
                req.getCostPrice(), req.getSellingPrice(), req.getDescription());
        return ResponseEntity.ok(ApiResponse.success("Bundle updated", bundle));
    }

    @PutMapping("/bundles/{id}/status")
    public ResponseEntity<ApiResponse<Bundle>> setBundleStatus(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null) throw new IllegalArgumentException("status is required");
        return ResponseEntity.ok(ApiResponse.success("Bundle status updated",
                bundleService.setStatus(id, status)));
    }

    @DeleteMapping("/bundles/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBundle(@PathVariable String id) {
        bundleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Bundle deleted", null));
    }

    // ── Orders ─────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<AdminOrderView>>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String network,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.plusDays(1).atStartOfDay() : null;

        boolean hasDate   = fromDt != null && toDt != null;
        boolean hasStatus = status  != null;
        boolean hasNet    = network != null;

        List<Order> orders;
        if (hasDate && hasStatus && hasNet)
            orders = orderRepository.findByStatusAndNetworkAndCreatedAtBetween(status.toUpperCase(), network.toLowerCase(), fromDt, toDt);
        else if (hasDate && hasStatus)
            orders = orderRepository.findByStatusAndCreatedAtBetween(status.toUpperCase(), fromDt, toDt);
        else if (hasDate && hasNet)
            orders = orderRepository.findByNetworkAndCreatedAtBetween(network.toLowerCase(), fromDt, toDt);
        else if (hasDate)
            orders = orderRepository.findByCreatedAtBetween(fromDt, toDt);
        else if (hasStatus && hasNet)
            orders = orderRepository.findByStatusAndNetwork(status.toUpperCase(), network.toLowerCase());
        else if (hasStatus)
            orders = orderRepository.findByStatus(status.toUpperCase());
        else if (hasNet)
            orders = orderRepository.findByNetwork(network.toLowerCase());
        else
            orders = orderRepository.findAll();

        return ResponseEntity.ok(ApiResponse.success(orders.stream().map(AdminOrderView::from).toList()));
    }

    // ── Analytics ──────────────────────────────────────────────────────────

    @GetMapping("/analytics/daily")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDailyAnalytics(
            @RequestParam(defaultValue = "30") int days) {

        LocalDateTime from = LocalDateTime.now().minusDays(days).toLocalDate().atStartOfDay();
        LocalDateTime to   = LocalDateTime.now().plusDays(1).toLocalDate().atStartOfDay();

        List<Order> orders = orderRepository.findByCreatedAtBetween(from, to);

        Map<String, List<Order>> byDate = orders.stream().collect(
                Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate().toString()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String date = LocalDateTime.now().minusDays(i).toLocalDate().toString();
            List<Order> dayOrders = byDate.getOrDefault(date, List.of());

            long totalOrders     = dayOrders.size();
            long completedOrders = dayOrders.stream().filter(o -> "COMPLETED".equals(o.getStatus())).count();

            // revenue = sum of sellingPrice (baseAmount) for completed orders
            // (Paystack fee is excluded — it goes directly to Paystack)
            BigDecimal revenue = dayOrders.stream()
                    .filter(o -> "COMPLETED".equals(o.getStatus()))
                    .map(o -> o.getBaseAmount() != null ? o.getBaseAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // profit = sellingPrice - costPrice  (agentCommission is added ON TOP of sellingPrice, so it doesn't reduce platform profit)
            BigDecimal profit = dayOrders.stream()
                    .filter(o -> "COMPLETED".equals(o.getStatus()))
                    .map(o -> {
                        BigDecimal selling = o.getBaseAmount()       != null ? o.getBaseAmount()       : BigDecimal.ZERO;
                        BigDecimal cost    = o.getCostPrice()        != null ? o.getCostPrice()        : BigDecimal.ZERO;
                        return selling.subtract(cost);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date",            date);
            row.put("orders",          totalOrders);
            row.put("completedOrders", completedOrders);
            row.put("revenue",         revenue);
            row.put("profit",          profit);
            result.add(row);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Stats ──────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        long totalUsers         = userRepository.count();
        long totalAgents        = agentProfileRepository.count();
        long totalBundles       = bundleService.getAll().size();
        long totalOrders        = orderRepository.count();
        long completedOrders    = orderRepository.findByStatus("COMPLETED").size();
        long failedOrders       = orderRepository.findByStatus("FAILED").size();
        long pendingWithdrawals = adminWithdrawalService.getByStatus("PENDING").size();

        List<Order> completed = orderRepository.findByStatus("COMPLETED");

        BigDecimal totalRevenue = completed.stream()
                .map(o -> o.getBaseAmount() != null ? o.getBaseAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfit = completed.stream()
                .map(o -> {
                    BigDecimal selling = o.getBaseAmount()       != null ? o.getBaseAmount()       : BigDecimal.ZERO;
                    BigDecimal cost    = o.getCostPrice()        != null ? o.getCostPrice()        : BigDecimal.ZERO;
                    return selling.subtract(cost);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "totalUsers",         totalUsers,
                "totalAgents",        totalAgents,
                "totalBundles",       totalBundles,
                "totalOrders",        totalOrders,
                "completedOrders",    completedOrders,
                "failedOrders",       failedOrders,
                "pendingWithdrawals", pendingWithdrawals,
                "totalRevenue",       totalRevenue,
                "totalProfit",        totalProfit
        )));
    }

    // ── Data Migration ──────────────────────────────────────────────────────

    /**
     * One-time backfill: patches costPrice and baseAmount on all existing orders
     * that were created before these fields were added.
     * Safe to call multiple times — already-patched orders are skipped.
     */
    @PostMapping("/migrations/backfill-order-costs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> backfillOrderCosts() {
        return ResponseEntity.ok(ApiResponse.success("Migration complete",
                orderMigrationService.backfillCostPrices()));
    }

    // ── DTOs ───────────────────────────────────────────────────────────────

    @Data
    public static class UpdateRolesRequest {
        private java.util.Set<String> roles;
    }

    @Data
    public static class UpdateBundleRequest {
        private String name;
        private String dataSize;
        private BigDecimal costPrice;
        private BigDecimal sellingPrice;
        private String description;
    }

    @Data
    public static class AdminNoteRequest {
        private String note;
    }
}

// File updated to trigger IDE reload

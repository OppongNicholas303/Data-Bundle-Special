package com.space.space_bundle.controller;

import com.space.space_bundle.dto.AdminOrderView;
import com.space.space_bundle.dto.ApiResponse;
import com.space.space_bundle.service.OrderMigrationService;
import com.space.space_bundle.dto.CreateBundleRequest;
import com.space.space_bundle.dto.AdminBundleResponse;
import com.space.space_bundle.entity.*;
import com.space.space_bundle.repository.*;
import com.space.space_bundle.service.AdminWithdrawalService;
import com.space.space_bundle.service.BundleService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final com.space.space_bundle.service.WalletService walletService;
    private final com.space.space_bundle.repository.ResultsTransactionRepository resultsTransactionRepository;
    private final com.space.space_bundle.repository.ResultCheckerPricingRepository resultCheckerPricingRepository;
    private final com.space.space_bundle.service.ResultsCheckerService resultsCheckerService;
    private final com.space.space_bundle.service.TransactionService transactionService;
    private final com.space.space_bundle.service.OrderService orderService;
    private final MongoTemplate mongoTemplate;
    private final com.space.space_bundle.repository.SmsPackageRepository smsPackageRepository;

    // ── Users ──────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userRepository.findAll(PageRequest.of(0, 1000)).getContent()));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/users/{id}/wallet")
    public ResponseEntity<ApiResponse<com.space.space_bundle.dto.WalletBalanceResponse>> getUserWallet(@PathVariable String id) {
        com.space.space_bundle.entity.Wallet wallet = walletService.getByUserId(id);
        return ResponseEntity.ok(ApiResponse.success(
                com.space.space_bundle.dto.WalletBalanceResponse.builder()
                        .balance(wallet.getBalance())
                        .commissionBalance(wallet.getCommissionBalance())
                        .currency("GHS")
                        .build()));
    }

    @GetMapping("/users/{id}/transactions")
    public ResponseEntity<ApiResponse<List<com.space.space_bundle.entity.Transaction>>> getUserTransactions(@PathVariable String id) {
        List<com.space.space_bundle.entity.Transaction> transactions = transactionService.getByUserId(id);
        // Sort descending by creation date
        transactions.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
    @PostMapping("/users/{id}/wallet/credit")
    public ResponseEntity<ApiResponse<String>> creditUserWallet(@PathVariable String id, @RequestBody com.space.space_bundle.dto.WalletAdjustmentRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero");
        }
        String desc = request.getDescription();
        if (desc == null || desc.isBlank()) desc = "Admin Credit";
        walletService.credit(id, request.getAmount(), desc);
        return ResponseEntity.ok(ApiResponse.success("Wallet credited successfully"));
    }

    @PostMapping("/users/{id}/wallet/debit")
    public ResponseEntity<ApiResponse<String>> debitUserWallet(@PathVariable String id, @RequestBody com.space.space_bundle.dto.WalletAdjustmentRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be greater than zero");
        }
        String desc = request.getDescription();
        if (desc == null || desc.isBlank()) desc = "Admin Debit";
        walletService.debit(id, request.getAmount(), null, desc); // null orderId for generic debit
        return ResponseEntity.ok(ApiResponse.success("Wallet debited successfully"));
    }

    @GetMapping("/results-checker/transactions")
    public ResponseEntity<ApiResponse<List<ResultsTransaction>>> getAllCheckerTransactions() {
        // Return all transactions sorted by creation date descending
        List<ResultsTransaction> transactions = resultsTransactionRepository.findAll();
        transactions.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @PostMapping("/results-checker/transactions/{referenceId}/retry")
    public ResponseEntity<ApiResponse<ResultsTransaction>> retryCheckerTransaction(@PathVariable String referenceId) {
        return resultsCheckerService.forceSyncTransaction(referenceId)
                .map(tx -> ResponseEntity.ok(ApiResponse.success(tx)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/results-checker/pricing")
    public ResponseEntity<ApiResponse<List<ResultCheckerPricing>>> getAllCheckerPricing() {
        return ResponseEntity.ok(ApiResponse.success(resultCheckerPricingRepository.findAll()));
    }

    @PutMapping("/results-checker/pricing/{serviceName}")
    public ResponseEntity<ApiResponse<ResultCheckerPricing>> updateCheckerRetailPrice(
            @PathVariable String serviceName,
            @RequestBody Map<String, Object> request) {
        
        ResultCheckerPricing pricing = resultCheckerPricingRepository.findById(serviceName)
                .orElse(ResultCheckerPricing.builder()
                        .serviceName(serviceName)
                        .amount(BigDecimal.ZERO)
                        .build());

        if (request.containsKey("retailPrice")) {
            Object val = request.get("retailPrice");
            if (val != null) {
                pricing.setRetailPrice(new BigDecimal(val.toString()));
            } else {
                pricing.setRetailPrice(null);
            }
        }
        pricing.setUpdatedAt(LocalDateTime.now());
        resultCheckerPricingRepository.save(pricing);

        return ResponseEntity.ok(ApiResponse.success(pricing));
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
        return ResponseEntity.ok(ApiResponse.success(agentProfileRepository.findAll(PageRequest.of(0, 1000)).getContent()));
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
        boolean activating = !agent.isActive();
        agent.setActive(activating);
        agentProfileRepository.save(agent);

        // When activating, grant ROLE_AGENT and ensure wallet exists.
        userRepository.findById(agent.getUserId()).ifPresent(user -> {
            if (activating) {
                if (user.getRoles() == null) user.setRoles(new java.util.HashSet<>());
                if (!user.getRoles().contains(com.space.space_bundle.entity.User.Role.ROLE_AGENT.name()))
                    user.getRoles().add(com.space.space_bundle.entity.User.Role.ROLE_AGENT.name());
                user.setUpdatedAt(java.time.LocalDateTime.now());
                userRepository.save(user);
                // create wallet if missing
                try {
                    walletService.createWallet(user.getId());
                } catch (Exception ignore) {
                    // wallet may already exist; ignore
                }
            } else {
                // Deactivating: remove ROLE_AGENT
                if (user.getRoles() != null && user.getRoles().contains(com.space.space_bundle.entity.User.Role.ROLE_AGENT.name())) {
                    user.getRoles().remove(com.space.space_bundle.entity.User.Role.ROLE_AGENT.name());
                    user.setUpdatedAt(java.time.LocalDateTime.now());
                    userRepository.save(user);
                }
            }
        });

        return ResponseEntity.ok(ApiResponse.success(
                agent.isActive() ? "Agent activated" : "Agent deactivated", agent));
    }

    @GetMapping("/agents/{id}/commissions")
    public ResponseEntity<ApiResponse<List<Commission>>> getAgentCommissions(
            @PathVariable String id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.plusDays(1).atStartOfDay() : null;

        boolean hasDate   = fromDt != null && toDt != null;
        boolean hasStatus = status != null;

        List<Commission> list;
        if (hasDate && hasStatus) {
            list = commissionRepository.findByAgentIdAndStatusAndCreatedAtBetween(id, status.toUpperCase(), fromDt, toDt);
        } else if (hasDate) {
            list = commissionRepository.findByAgentIdAndCreatedAtBetween(id, fromDt, toDt);
        } else if (hasStatus) {
            list = commissionRepository.findByAgentIdAndStatus(id, status.toUpperCase());
        } else {
            list = commissionRepository.findByAgentId(id);
        }

        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/agents/{id}/orders")
    public ResponseEntity<ApiResponse<List<AdminOrderView>>> getAgentOrders(
            @PathVariable String id,
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
            orders = orderRepository.findByStatusAndNetworkAndCreatedAtBetween(status.toUpperCase(), network.toLowerCase(), fromDt, toDt).stream().filter(o -> id.equals(o.getAgentId())).toList();
        else if (hasDate && hasStatus)
            orders = orderRepository.findByStatusAndCreatedAtBetween(status.toUpperCase(), fromDt, toDt).stream().filter(o -> id.equals(o.getAgentId())).toList();
        else if (hasDate && hasNet)
            orders = orderRepository.findByNetworkAndCreatedAtBetween(network.toLowerCase(), fromDt, toDt).stream().filter(o -> id.equals(o.getAgentId())).toList();
        else if (hasDate)
            orders = orderRepository.findByCreatedAtBetween(fromDt, toDt).stream().filter(o -> id.equals(o.getAgentId())).toList();
        else if (hasStatus && hasNet)
            orders = orderRepository.findByStatusAndNetwork(status.toUpperCase(), network.toLowerCase()).stream().filter(o -> id.equals(o.getAgentId())).toList();
        else if (hasStatus)
            orders = orderRepository.findByAgentIdAndStatus(id, status.toUpperCase());
        else if (hasNet)
            orders = orderRepository.findByNetwork(network.toLowerCase()).stream().filter(o -> id.equals(o.getAgentId())).toList();
        else
            orders = orderRepository.findByAgentId(id);

        return ResponseEntity.ok(ApiResponse.success(orders.stream().map(AdminOrderView::from).toList()));
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

    @GetMapping("/agents/{id}/wallet")
    public ResponseEntity<ApiResponse<com.space.space_bundle.dto.WalletBalanceResponse>> getAgentWallet(@PathVariable String id) {
        AgentProfile agent = agentProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));
        com.space.space_bundle.entity.Wallet wallet = walletService.getByUserId(agent.getUserId());
        return ResponseEntity.ok(ApiResponse.success(
                com.space.space_bundle.dto.WalletBalanceResponse.builder()
                        .balance(wallet.getBalance())
                        .commissionBalance(wallet.getCommissionBalance())
                        .currency("GHS").build()));
    }

    @PostMapping("/agents/{id}/wallet/topup")
    public ResponseEntity<ApiResponse<com.space.space_bundle.dto.WalletBalanceResponse>> topUpAgentWallet(
            @PathVariable String id, @RequestBody AdminTopUpRequest req) {
        if (req.getAmount() == null) throw new IllegalArgumentException("amount is required");
        if (req.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amount must be > 0");
        AgentProfile agent = agentProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));
        // Use admin provided note if present, otherwise fall back to default description
        String desc = req.getNote() != null && !req.getNote().isBlank()
                ? req.getNote()
                : "Admin top-up for agent " + agent.getBusinessName();
        walletService.credit(agent.getUserId(), req.getAmount(), desc);
        java.math.BigDecimal balance = walletService.getBalance(agent.getUserId());
        return ResponseEntity.ok(ApiResponse.success(
                com.space.space_bundle.dto.WalletBalanceResponse.builder()
                        .balance(balance).currency("GHS").build()));
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
    public ResponseEntity<ApiResponse<List<AdminBundleResponse>>> getAllBundles() {
        List<AdminBundleResponse> res = bundleService.getAll().stream()
                .map(AdminBundleResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @PostMapping("/bundles")
    public ResponseEntity<ApiResponse<AdminBundleResponse>> createBundle(@RequestBody CreateBundleRequest req) {
        Bundle bundle = bundleService.create(req.getCode(), req.getName(), req.getDataSize(),
                req.getNetwork(), req.getCostPrice(), req.getSellingPrice(), req.getDescription(), req.getPreferredProvider());
        return ResponseEntity.ok(ApiResponse.success("Bundle created", AdminBundleResponse.from(bundle)));
    }

    @PutMapping("/bundles/{id}")
    public ResponseEntity<ApiResponse<AdminBundleResponse>> updateBundle(
            @PathVariable String id, @RequestBody UpdateBundleRequest req) {
        Bundle bundle = bundleService.update(id, req.getName(), req.getDataSize(),
                req.getCostPrice(), req.getSellingPrice(), req.getDescription(), req.getPreferredProvider());
        return ResponseEntity.ok(ApiResponse.success("Bundle updated", AdminBundleResponse.from(bundle)));
    }

    @PutMapping("/bundles/{id}/status")
    public ResponseEntity<ApiResponse<AdminBundleResponse>> setBundleStatus(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null) throw new IllegalArgumentException("status is required");
        return ResponseEntity.ok(ApiResponse.success("Bundle status updated",
                AdminBundleResponse.from(bundleService.setStatus(id, status))));
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
         // Hard limit to 1000 to prevent OOM without breaking frontend Array response structure
         Pageable limit = PageRequest.of(0, 1000);

         if (hasStatus && hasNet && hasDate)
             orders = orderRepository.findByStatusAndNetworkAndCreatedAtBetween(status.toUpperCase(), network.toLowerCase(), fromDt, toDt, limit);
         else if (hasStatus && hasDate)
             orders = orderRepository.findByStatusAndCreatedAtBetween(status.toUpperCase(), fromDt, toDt, limit);
         else if (hasDate && hasNet)
             orders = orderRepository.findByNetworkAndCreatedAtBetween(network.toLowerCase(), fromDt, toDt, limit);
         else if (hasDate)
             orders = orderRepository.findByCreatedAtBetween(fromDt, toDt, limit);
         else if (hasStatus && hasNet)
             orders = orderRepository.findByStatusAndNetwork(status.toUpperCase(), network.toLowerCase(), limit);
         else if (hasStatus)
             orders = orderRepository.findByStatus(status.toUpperCase(), limit);
         else if (hasNet)
             orders = orderRepository.findByNetwork(network.toLowerCase(), limit);
         else
             orders = orderRepository.findAll(limit).getContent();

         return ResponseEntity.ok(ApiResponse.success(
                 orders.stream()
                         .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                         .map(AdminOrderView::from)
                         .collect(Collectors.toList())
         ));
     }

     @GetMapping("/orders/phone-numbers")
     public ResponseEntity<ApiResponse<List<String>>> getDistinctOrderPhoneNumbers(
             @RequestParam(required = false) String status,
             @RequestParam(required = false) String network) {

         Query query = new Query();
         if (status != null && !status.isBlank()) {
             query.addCriteria(Criteria.where("status").is(status.toUpperCase()));
         }
         if (network != null && !network.isBlank()) {
             query.addCriteria(Criteria.where("network").is(network.toUpperCase()));
         }

         List<String> phoneNumbers = mongoTemplate.findDistinct(query, "phoneNumber", Order.class, String.class);
         return ResponseEntity.ok(ApiResponse.success(phoneNumbers));
     }

     // ── Transactions ────────────────────────────────────────────────────────

     @GetMapping("/transactions")
     public ResponseEntity<ApiResponse<List<com.space.space_bundle.dto.AdminTransactionView>>> getAllTransactions(
             @RequestParam(required = false) String status,
             @RequestParam(required = false) String type,
             @RequestParam(required = false) String search,
             @RequestParam(required = false) String fromDate,
             @RequestParam(required = false) String toDate) {
         
         List<com.space.space_bundle.dto.AdminTransactionView> transactions = transactionService.getAllTransactions(status, type, search, fromDate, toDate);
         return ResponseEntity.ok(ApiResponse.success(transactions));
     }

     @PostMapping("/orders/{id}/mark-complete")
     public ResponseEntity<ApiResponse<AdminOrderView>> markOrderComplete(
             @PathVariable String id,
             @RequestBody(required = false) Map<String, String> body) {
         Order order = orderRepository.findById(id)
                 .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

         if (!"PROCESSING".equals(order.getStatus())) {
             throw new IllegalArgumentException("Only orders in PROCESSING status can be marked as complete. Current status: " + order.getStatus());
         }

         String providerReference = body != null && body.containsKey("providerReference")
                 ? body.get("providerReference")
                 : "MANUAL_" + UUID.randomUUID().toString().substring(0, 8);

         order.markCompleted(providerReference);
         orderRepository.save(order);

      return ResponseEntity.ok(ApiResponse.success("Order marked as complete", AdminOrderView.from(order)));
      }

      @PostMapping("/orders/{id}/reprocess")
      public ResponseEntity<ApiResponse<AdminOrderView>> reprocessOrder(@PathVariable String id) {
          Order order = orderService.reprocessFailedOrder(id);
          return ResponseEntity.ok(ApiResponse.success("Order reprocessed successfully", AdminOrderView.from(order)));
      }

      @PostMapping("/orders/{id}/mark-complete-by-admin")
      public ResponseEntity<ApiResponse<AdminOrderView>> markOrderCompleteByAdmin(
              @PathVariable String id,
              @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
          Order order = orderRepository.findById(id)
                  .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

          String adminId = userDetails != null ? userDetails.getUsername() : "unknown-admin";

          try {
              order.markCompleteByAdmin(adminId);
              order = orderRepository.save(order);
              orderService.settleCommission(order);
              return ResponseEntity.ok(ApiResponse.success("Order marked as COMPLETE_BY_ADMIN by admin", AdminOrderView.from(order)));
          } catch (IllegalStateException e) {
              throw new IllegalArgumentException(e.getMessage());
          }
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
             long completedOrders = dayOrders.stream().filter(o -> "COMPLETED".equals(o.getStatus()) || "COMPLETE_BY_ADMIN".equals(o.getStatus())).count();

             // revenue = sum of sellingPrice (baseAmount) for completed orders
             // (Moolre fee is excluded — it goes directly to Moolre)
             BigDecimal revenue = dayOrders.stream()
                     .filter(o -> "COMPLETED".equals(o.getStatus()) || "COMPLETE_BY_ADMIN".equals(o.getStatus()))
                     .map(o -> o.getBaseAmount() != null ? o.getBaseAmount() : BigDecimal.ZERO)
                     .reduce(BigDecimal.ZERO, BigDecimal::add);

             // profit = sellingPrice - costPrice  (agentCommission is added ON TOP of sellingPrice, so it doesn't reduce platform profit)
             BigDecimal profit = dayOrders.stream()
                     .filter(o -> "COMPLETED".equals(o.getStatus()) || "COMPLETE_BY_ADMIN".equals(o.getStatus()))
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

     @GetMapping("/analytics/agent-commissions")
     public ResponseEntity<ApiResponse<Map<String, Object>>> getAgentCommissionsSummary(
             @RequestParam(required = false) String agentId,
             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

         LocalDateTime fromDt = from != null ? from.atStartOfDay() : LocalDateTime.now().minusDays(30).toLocalDate().atStartOfDay();
         LocalDateTime toDt   = to   != null ? to.plusDays(1).atStartOfDay() : LocalDateTime.now().plusDays(1).toLocalDate().atStartOfDay();

         List<Commission> commissions;
         if (agentId != null && !agentId.isBlank()) {
             commissions = commissionRepository.findByAgentIdAndCreatedAtBetween(agentId, fromDt, toDt);
         } else {
             commissions = commissionRepository.findByCreatedAtBetween(fromDt, toDt);
         }

         // Group by date
         Map<String, List<Commission>> byDate = commissions.stream()
                 .collect(Collectors.groupingBy(c -> c.getCreatedAt().toLocalDate().toString()));

         // Build daily summary
         List<Map<String, Object>> dailySummary = new ArrayList<>();
         LocalDate current = fromDt.toLocalDate();
         LocalDate end = toDt.toLocalDate();

         while (!current.isAfter(end)) {
             String dateStr = current.toString();
             List<Commission> dayCommissions = byDate.getOrDefault(dateStr, List.of());

             BigDecimal totalCommission = dayCommissions.stream()
                     .map(c -> c.getProfit() != null ? c.getProfit() : BigDecimal.ZERO)
                     .reduce(BigDecimal.ZERO, BigDecimal::add);

             if (!dayCommissions.isEmpty() || !dailySummary.isEmpty()) {
                 Map<String, Object> day = new LinkedHashMap<>();
                 day.put("date", dateStr);
                 day.put("count", dayCommissions.size());
                 day.put("totalCommission", totalCommission);
                 dailySummary.add(day);
             }
             current = current.plusDays(1);
         }

         // Overall totals
         BigDecimal grandTotal = commissions.stream()
                 .map(c -> c.getProfit() != null ? c.getProfit() : BigDecimal.ZERO)
                 .reduce(BigDecimal.ZERO, BigDecimal::add);

         return ResponseEntity.ok(ApiResponse.success(Map.of(
                 "dailySummary", dailySummary,
                 "totalCommissions", grandTotal,
                 "totalOrders", commissions.size(),
                 "fromDate", from != null ? from.toString() : fromDt.toLocalDate().toString(),
                 "toDate", to != null ? to.toString() : toDt.toLocalDate().toString(),
                 "agentId", agentId != null ? agentId : "all"
         )));
     }

    // ── Stats ──────────────────────────────────────────────────────────────

    @Data
    private static class StatsAggregationResult {
        private long count;
        private BigDecimal totalRevenue;
        private BigDecimal totalCost;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        long totalUsers         = userRepository.count();
        long totalAgents        = agentProfileRepository.count();
        long totalBundles       = bundleService.getAll().size();
        long totalOrders        = orderRepository.count();
        long failedOrders       = orderRepository.findByStatus("FAILED").size();
        long pendingWithdrawals = adminWithdrawalService.getByStatus("PENDING").size();

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("status").in("COMPLETED", "COMPLETE_BY_ADMIN")),
                Aggregation.group()
                        .count().as("count")
                        .sum("baseAmount").as("totalRevenue")
                        .sum("costPrice").as("totalCost")
        );

        AggregationResults<StatsAggregationResult> results = mongoTemplate.aggregate(agg, "orders", StatsAggregationResult.class);
        StatsAggregationResult statsResult = results.getUniqueMappedResult();

        long completedOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        if (statsResult != null) {
            completedOrders = statsResult.getCount();
            totalRevenue = statsResult.getTotalRevenue() != null ? statsResult.getTotalRevenue() : BigDecimal.ZERO;
            BigDecimal totalCost = statsResult.getTotalCost() != null ? statsResult.getTotalCost() : BigDecimal.ZERO;
            totalProfit = totalRevenue.subtract(totalCost);
        }

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

    // ── SMS Packages ───────────────────────────────────────────────────────

    @GetMapping("/sms-packages")
    public ResponseEntity<ApiResponse<List<SmsPackage>>> getAllSmsPackages() {
        return ResponseEntity.ok(ApiResponse.success(smsPackageRepository.findAllByOrderByPriceAsc()));
    }

    @PostMapping("/sms-packages")
    public ResponseEntity<ApiResponse<SmsPackage>> createSmsPackage(@RequestBody SmsPackage pkg) {
        if (pkg.getName() == null || pkg.getName().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Package name is required"));
        }
        if (pkg.getMessagesCount() <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Messages count must be positive"));
        }
        if (pkg.getPrice() == null || pkg.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Price must be greater than zero"));
        }
        pkg.setId(UUID.randomUUID().toString());
        pkg.setCreatedAt(LocalDateTime.now());
        pkg.setUpdatedAt(LocalDateTime.now());
        SmsPackage saved = smsPackageRepository.save(pkg);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PutMapping("/sms-packages/{id}")
    public ResponseEntity<ApiResponse<SmsPackage>> updateSmsPackage(
            @PathVariable String id,
            @RequestBody SmsPackage pkgDetails) {
        SmsPackage existing = smsPackageRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("SMS package not found"));
        }
        if (pkgDetails.getName() != null && !pkgDetails.getName().isBlank()) {
            existing.setName(pkgDetails.getName());
        }
        if (pkgDetails.getMessagesCount() > 0) {
            existing.setMessagesCount(pkgDetails.getMessagesCount());
        }
        if (pkgDetails.getPrice() != null && pkgDetails.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            existing.setPrice(pkgDetails.getPrice());
        }
        existing.setActive(pkgDetails.isActive());
        existing.setUpdatedAt(LocalDateTime.now());
        SmsPackage saved = smsPackageRepository.save(existing);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @DeleteMapping("/sms-packages/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSmsPackage(@PathVariable String id) {
        if (!smsPackageRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("SMS package not found"));
        }
        smsPackageRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null));
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
        private String preferredProvider;
    }

    @Data
    public static class AdminTopUpRequest {
        private java.math.BigDecimal amount;
        // Optional admin note to be saved with the wallet transaction
        private String note;
    }

    @Data
    public static class AdminNoteRequest {
        private String note;
    }
}

// File updated to trigger IDE reload

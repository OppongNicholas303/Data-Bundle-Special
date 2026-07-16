package com.space.space_bundle.service;

import com.space.space_bundle.dto.checkerport.*;
import com.space.space_bundle.entity.ResultCheckerPricing;
import com.space.space_bundle.entity.ResultsTransaction;
import com.space.space_bundle.entity.ResultsTransactionType;
import com.space.space_bundle.entity.ServiceStatus;
import com.space.space_bundle.repository.ResultCheckerPricingRepository;
import com.space.space_bundle.repository.ResultsTransactionRepository;
import com.space.space_bundle.repository.AgentProfileRepository;
import com.space.space_bundle.repository.AgentCheckerPricingRepository;
import com.space.space_bundle.entity.AgentProfile;
import com.space.space_bundle.entity.AgentCheckerPricing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultsCheckerService {

    private final CheckerPortClient checkerPortClient;
    private final ResultsTransactionRepository transactionRepository;
    private final ResultCheckerPricingRepository pricingRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final AgentCheckerPricingRepository agentCheckerPricingRepository;
    private final EmailService emailService;
    private final com.space.space_bundle.service.CommissionService commissionService;

    public ResultsTransaction buyVoucher(CheckerPortVoucherRequest request, String userId) {
        // Validate request
        if (request.getQty() == null || request.getQty() < 1 || request.getQty() > 200) {
            throw new IllegalArgumentException("Quantity must be between 1 and 200");
        }

        String pricingKey = "VoucherPrice" + request.getPlatform();
        ResultCheckerPricing pricing = pricingRepository.findById(pricingKey)
                .orElseThrow(() -> new IllegalStateException("Pricing not configured for " + request.getPlatform()));
        
        if (pricing.getRetailPrice() == null) {
            throw new IllegalStateException("Retail price not set by Admin for " + request.getPlatform());
        }

        // SECURITY: Override the request price with the Admin's configured retail price!
        request.setPrice(pricing.getRetailPrice());

        // Check for Agent Pricing Override
        String agentId = null;
        BigDecimal agentProfit = BigDecimal.ZERO;
        if (request.getAgentCode() != null && !request.getAgentCode().trim().isEmpty()) {
            Optional<AgentProfile> agentOpt = agentProfileRepository.findByReferralCode(request.getAgentCode().trim());
            if (agentOpt.isPresent()) {
                AgentProfile agent = agentOpt.get();
                agentId = agent.getId();
            }
        } else if (userId != null) {
            Optional<AgentProfile> agentOpt = agentProfileRepository.findByUserId(userId);
            if (agentOpt.isPresent()) {
                AgentProfile agent = agentOpt.get();
                agentId = agent.getId();
            }
        }
        
        if (agentId != null) {
            Optional<AgentCheckerPricing> agentPricingOpt = agentCheckerPricingRepository.findByAgentIdAndServiceName(agentId, pricingKey);
                
            if (agentPricingOpt.isPresent() && agentPricingOpt.get().isActive()) {
                AgentCheckerPricing agentPricing = agentPricingOpt.get();
                // SECURITY: Override with Agent's selling price
                request.setPrice(agentPricing.getSellingPrice());
                agentProfit = agentPricing.calculateProfit();
                    
                // Multiply profit by quantity!
                if (request.getQty() != null && request.getQty() > 1) {
                    agentProfit = agentProfit.multiply(BigDecimal.valueOf(request.getQty()));
                }
            }
        }

        // We can fetch price from DB or trust the request
        CheckerPortResponse<Map<String, Object>> response = checkerPortClient.buyVoucher(request);

        ResultsTransaction transaction = ResultsTransaction.builder()
                .referenceId(request.getReferenceId())
                .userId(userId)
                .agentId(agentId)
                .agentProfit(agentProfit)
                .type(ResultsTransactionType.VOUCHER)
                .status(ServiceStatus.PENDING)
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .price(request.getPrice())
                .qty(request.getQty())
                .platform(request.getPlatform())
                .costPrice(pricing.getAmount()) // original cost from CheckerPort

                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (response.getData() != null && response.getData().containsKey("referenceId")) {
            transaction.setReferenceId((String) response.getData().get("referenceId"));
        }

        return transactionRepository.save(transaction);
    }

    public ResultsTransaction checkResult(CheckerPortArcRequest request, String userId) {
        // Conditional validation
        if (!"ShsPlacement".equalsIgnoreCase(request.getType()) && request.getYear() == null) {
            throw new IllegalArgumentException("Year is required for this result type");
        }
        if (("WasscePrivate".equalsIgnoreCase(request.getType()) || "ShsPlacement".equalsIgnoreCase(request.getType())) && request.getDob() == null) {
            throw new IllegalArgumentException("DOB is required for this result type");
        }

        String pricingKey = switch (request.getType()) {
            case "Bece" -> "ArcPriceWrcBeceSchool";
            case "WassceSchool" -> "ArcPriceWrcWassceSchool";
            case "WasscePrivate" -> "ArcPriceWrcWasscePrivate";
            case "ShsPlacement" -> "ArcPriceSpr";
            default -> throw new IllegalArgumentException("Invalid result type");
        };

        ResultCheckerPricing pricing = pricingRepository.findById(pricingKey)
                .orElseThrow(() -> new IllegalStateException("Pricing not configured for " + request.getType()));

        if (pricing.getRetailPrice() == null) {
            throw new IllegalStateException("Retail price not set by Admin for " + request.getType());
        }

        // SECURITY: Override the request price with the Admin's configured retail price!
        request.setPrice(pricing.getRetailPrice());

        // Check for Agent Pricing Override
        String agentId = null;
        BigDecimal agentProfit = BigDecimal.ZERO;
        if (request.getAgentCode() != null && !request.getAgentCode().trim().isEmpty()) {
            Optional<AgentProfile> agentOpt = agentProfileRepository.findByReferralCode(request.getAgentCode().trim());
            if (agentOpt.isPresent()) {
                AgentProfile agent = agentOpt.get();
                agentId = agent.getId();
            }
        } else if (userId != null) {
            Optional<AgentProfile> agentOpt = agentProfileRepository.findByUserId(userId);
            if (agentOpt.isPresent()) {
                AgentProfile agent = agentOpt.get();
                agentId = agent.getId();
            }
        }

        if (agentId != null) {
            Optional<AgentCheckerPricing> agentPricingOpt = agentCheckerPricingRepository.findByAgentIdAndServiceName(agentId, pricingKey);
                
            if (agentPricingOpt.isPresent() && agentPricingOpt.get().isActive()) {
                AgentCheckerPricing agentPricing = agentPricingOpt.get();
                // SECURITY: Override with Agent's selling price
                request.setPrice(agentPricing.getSellingPrice());
                agentProfit = agentPricing.calculateProfit();
            }
        }

        CheckerPortResponse<Map<String, Object>> response = checkerPortClient.checkResult(request);

        ResultsTransactionType tType = switch (request.getType()) {
            case "Bece" -> ResultsTransactionType.ARC_BECE;
            case "WassceSchool" -> ResultsTransactionType.ARC_WASSCE_SCHOOL;
            case "WasscePrivate" -> ResultsTransactionType.ARC_WASSCE_PRIVATE;
            case "ShsPlacement" -> ResultsTransactionType.ARC_SHS_PLACEMENT;
            default -> throw new IllegalArgumentException("Invalid result type");
        };

        ResultsTransaction transaction = ResultsTransaction.builder()
                .referenceId(request.getReferenceId())
                .userId(userId)
                .agentId(agentId)
                .agentProfit(agentProfit)
                .type(tType)
                .status(ServiceStatus.PENDING)
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .price(request.getPrice())
                .costPrice(pricing.getAmount()) // original cost from CheckerPort
                .indexNumber(request.getIndexNumber())
                .year(request.getYear())
                .dob(request.getDob())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (response.getData() != null && response.getData().containsKey("referenceId")) {
            transaction.setReferenceId((String) response.getData().get("referenceId"));
        }

        return transactionRepository.save(transaction);
    }

    public ResultsTransaction submitCorrection(String referenceId, CheckerPortCorrectionRequest request) {
        ResultsTransaction tx = transactionRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (tx.getStatus() != ServiceStatus.PENDING_INPUT) {
            throw new IllegalStateException("Transaction is not in pending-input state");
        }

        request.setReferenceId(referenceId);
        checkerPortClient.submitCorrection(request);

        tx.setStatus(ServiceStatus.PENDING);
        tx.setIndexNumber(request.getIndexNumber() != null ? request.getIndexNumber() : tx.getIndexNumber());
        tx.setDob(request.getDob() != null ? request.getDob() : tx.getDob());
        tx.setYear(request.getYear() != null ? request.getYear() : tx.getYear());
        tx.setUpdatedAt(LocalDateTime.now());

        return transactionRepository.save(tx);
    }

    public Optional<ResultsTransaction> getTransaction(String referenceId) {
        Optional<ResultsTransaction> txOpt = transactionRepository.findByReferenceId(referenceId);
        if (txOpt.isPresent()) {
            ResultsTransaction tx = txOpt.get();
            if (tx.getStatus() == ServiceStatus.PENDING &&
                tx.getCreatedAt() != null &&
                tx.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
                
                try {
                    CheckerPortResponse<Map<String, Object>> resp = checkerPortClient.getStatus(referenceId);
                    if ("SUCCESS".equalsIgnoreCase(resp.getStatus()) && resp.getData() != null) {
                        Map<String, Object> data = resp.getData();
                        String serviceStatus = (String) data.get("serviceStatus");
                        
                        if ("complete".equalsIgnoreCase(serviceStatus)) {
                            tx.setStatus(ServiceStatus.COMPLETE);
                        } else if ("pending-input".equalsIgnoreCase(serviceStatus)) {
                            tx.setStatus(ServiceStatus.PENDING_INPUT);
                        }
                        
                        tx.setStatusCode((String) data.get("statusCode"));
                        
                        if (data.containsKey("result")) {
                            tx.setResultData((Map<String, Object>) data.get("result"));
                        }
                        if (data.containsKey("vouchers")) {
                            tx.setVouchers(data.get("vouchers"));
                        }
                        
                        tx.setUpdatedAt(LocalDateTime.now());
                        tx = transactionRepository.save(tx);
                        
                        if ("complete".equalsIgnoreCase(serviceStatus) && tx.getEmail() != null && !tx.getEmail().isEmpty()) {
                            sendDeliveryEmail(tx);
                        }
                        
                        if ("complete".equalsIgnoreCase(serviceStatus) && tx.getAgentId() != null && !tx.isCommissionPaid() && tx.getAgentProfit() != null && tx.getAgentProfit().compareTo(BigDecimal.ZERO) > 0) {
                            try {
                                BigDecimal baseAmount = tx.getPrice().subtract(tx.getAgentProfit());
                                commissionService.settle(tx.getAgentId(), tx.getId(), baseAmount, tx.getPrice());
                                tx.setCommissionPaid(true);
                                tx = transactionRepository.save(tx);
                            } catch (Exception e) {
                                log.error("Failed to settle commission for tx {}: {}", tx.getId(), e.getMessage());
                            }
                        }

                        return Optional.of(tx);
                    }
                } catch (Exception e) {
                    log.error("JIT status check failed for {}: {}", referenceId, e.getMessage());
                }
            }
            return Optional.of(tx);
        }
        return Optional.empty();
    }

    public Optional<ResultsTransaction> forceSyncTransaction(String referenceId) {
        Optional<ResultsTransaction> txOpt = transactionRepository.findByReferenceId(referenceId);
        if (txOpt.isPresent()) {
            ResultsTransaction tx = txOpt.get();
            try {
                CheckerPortResponse<Map<String, Object>> resp = checkerPortClient.getStatus(referenceId);
                if ("SUCCESS".equalsIgnoreCase(resp.getStatus()) && resp.getData() != null) {
                    Map<String, Object> data = resp.getData();
                    String serviceStatus = (String) data.get("serviceStatus");
                    
                    if ("complete".equalsIgnoreCase(serviceStatus)) {
                        tx.setStatus(ServiceStatus.COMPLETE);
                    } else if ("pending-input".equalsIgnoreCase(serviceStatus)) {
                        tx.setStatus(ServiceStatus.PENDING_INPUT);
                    } else if ("failed".equalsIgnoreCase(serviceStatus)) {
                        tx.setStatus(ServiceStatus.FAILED);
                    }
                    
                    tx.setStatusCode((String) data.get("statusCode"));
                    
                    if (data.containsKey("result")) {
                        tx.setResultData((Map<String, Object>) data.get("result"));
                    }
                    if (data.containsKey("vouchers")) {
                        tx.setVouchers(data.get("vouchers"));
                    }
                    
                    tx.setUpdatedAt(LocalDateTime.now());
                    tx = transactionRepository.save(tx);
                    
                    // Trigger email if complete and not previously triggered? 
                    // To prevent duplicate emails, we assume forceSync might re-trigger if needed, 
                    // or we check if email was already sent. But sending again is fine for a manual force sync.
                    if ("complete".equalsIgnoreCase(serviceStatus) && tx.getEmail() != null && !tx.getEmail().isEmpty()) {
                        sendDeliveryEmail(tx);
                    }
                    
                    if ("complete".equalsIgnoreCase(serviceStatus) && tx.getAgentId() != null && !tx.isCommissionPaid() && tx.getAgentProfit() != null && tx.getAgentProfit().compareTo(BigDecimal.ZERO) > 0) {
                        try {
                            BigDecimal baseAmount = tx.getPrice().subtract(tx.getAgentProfit());
                            commissionService.settle(tx.getAgentId(), tx.getId(), baseAmount, tx.getPrice());
                            tx.setCommissionPaid(true);
                            tx = transactionRepository.save(tx);
                        } catch (Exception e) {
                            log.error("Failed to settle commission for tx {}: {}", tx.getId(), e.getMessage());
                        }
                    }
                    
                    return Optional.of(tx);
                }
            } catch (Exception e) {
                log.error("Force sync failed for {}: {}", referenceId, e.getMessage());
            }
            return Optional.of(tx);
        }
        return Optional.empty();
    }

    public Iterable<ResultsTransaction> getTransactionsByUserId(String userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @PostConstruct
    @Scheduled(cron = "0 0 0 * * ?")
    public void updatePricingFromApi() {
        String[] services = {"VoucherPricePlatformWaecNew", "VoucherPricePlatformWaecOld",
                "ArcPriceWrcBeceSchool", "ArcPriceWrcWassceSchool", "ArcPriceWrcWasscePrivate", "ArcPriceSpr"};

        for (String s : services) {
            // Ensure the row ALWAYS exists so admins can set Retail Price, even if the API fetch fails
            ResultCheckerPricing pricing = pricingRepository.findById(s).orElse(
                ResultCheckerPricing.builder()
                    .serviceName(s)
                    .amount(BigDecimal.ZERO)
                    .retailPrice(new BigDecimal("30.00"))
                    .build()
            );
            if (pricing.getRetailPrice() == null) {
                pricing.setRetailPrice(new BigDecimal("30.00"));
            }
            pricingRepository.save(pricing);

            try {
                CheckerPortResponse<Map<String, Object>> resp = checkerPortClient.getServicePrice(s);
                if ("SUCCESS".equalsIgnoreCase(resp.getStatus()) && resp.getData() != null) {
                    Object priceObj = resp.getData().get("price");
                    if (priceObj != null) {
                        BigDecimal amount = new BigDecimal(priceObj.toString());
                        pricing.setAmount(amount);
                        pricing.setUpdatedAt(LocalDateTime.now());
                        pricingRepository.save(pricing);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch pricing for {}: {}", s, e.getMessage());
            }
        }
    }

    public Iterable<ResultCheckerPricing> getAllPricing() {
        return pricingRepository.findAll();
    }

    public void handleWebhook(Map<String, Object> payload) {
        if (payload == null) return;

        String status = (String) payload.get("status");
        if ("FAILED".equalsIgnoreCase(status)) {
            log.error("Received FAILED webhook from CheckerPort: {}", payload);
            return;
        }

        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null) return;

        String referenceId = (String) data.get("referenceId");
        if (referenceId == null) return;

        Optional<ResultsTransaction> txOpt = transactionRepository.findByReferenceId(referenceId);
        if (txOpt.isEmpty()) {
            log.warn("Webhook received for unknown referenceId: {}", referenceId);
            return;
        }

        ResultsTransaction tx = txOpt.get();
        String serviceStatus = (String) data.get("serviceStatus");
        
        if ("complete".equalsIgnoreCase(serviceStatus)) {
            tx.setStatus(ServiceStatus.COMPLETE);
        } else if ("pending-input".equalsIgnoreCase(serviceStatus)) {
            tx.setStatus(ServiceStatus.PENDING_INPUT);
        } else if ("failed".equalsIgnoreCase(serviceStatus)) {
            tx.setStatus(ServiceStatus.FAILED);
        }

        tx.setStatusCode((String) data.get("statusCode"));

        if (data.containsKey("result")) {
            tx.setResultData((Map<String, Object>) data.get("result"));
        }
        if (data.containsKey("vouchers")) {
            tx.setVouchers(data.get("vouchers"));
        }

        tx.setUpdatedAt(LocalDateTime.now());
        tx = transactionRepository.save(tx);
        log.info("Successfully processed webhook for referenceId: {}, new status: {}", referenceId, serviceStatus);
        
        if ("complete".equalsIgnoreCase(serviceStatus) && tx.getEmail() != null && !tx.getEmail().isEmpty()) {
            sendDeliveryEmail(tx);
        }

        if ("complete".equalsIgnoreCase(serviceStatus) && tx.getAgentId() != null && !tx.isCommissionPaid() && tx.getAgentProfit() != null && tx.getAgentProfit().compareTo(BigDecimal.ZERO) > 0) {
            try {
                BigDecimal baseAmount = tx.getPrice().subtract(tx.getAgentProfit());
                commissionService.settle(tx.getAgentId(), tx.getId(), baseAmount, tx.getPrice());
                tx.setCommissionPaid(true);
                tx = transactionRepository.save(tx);
            } catch (Exception e) {
                log.error("Failed to settle commission for tx {}: {}", tx.getId(), e.getMessage());
            }
        }
    }

    private void sendDeliveryEmail(ResultsTransaction tx) {
        String subject = "Your WAEC Checker/Voucher Order: " + tx.getReferenceId();
        StringBuilder textBody = new StringBuilder("Hello,\n\nYour order is complete!\n\n");
        StringBuilder htmlBody = new StringBuilder("<h3>Your order is complete!</h3>");

        if (tx.getVouchers() != null) {
            java.util.List<Map<String, Object>> vouchers = (java.util.List<Map<String, Object>>) tx.getVouchers();
            textBody.append("Vouchers:\n");
            for (Map<String, Object> v : vouchers) {
                textBody.append("Serial: ").append(v.get("serial")).append(" | PIN: ").append(v.get("pin")).append("\n");
                htmlBody.append("<p><b>Serial:</b> ").append(v.get("serial")).append(" <br/><b>PIN:</b> ").append(v.get("pin")).append("</p>");
            }
        } else if (tx.getResultData() != null) {
            Map<String, Object> resultContent = (Map<String, Object>) tx.getResultData().get("resultContent");
            if (resultContent != null && resultContent.get("pdfUrl") != null) {
                String pdf = (String) resultContent.get("pdfUrl");
                textBody.append("Result PDF: ").append(pdf).append("\n");
                htmlBody.append("<p><a href='").append(pdf).append("'>Download Result PDF</a></p>");
            }
            Map<String, Object> placementDetails = (Map<String, Object>) tx.getResultData().get("placementDetails");
            if (placementDetails != null && placementDetails.get("pdfs") != null) {
                java.util.List<Map<String, Object>> pdfs = (java.util.List<Map<String, Object>>) placementDetails.get("pdfs");
                for (Map<String, Object> pdf : pdfs) {
                    String url = (String) pdf.get("url");
                    String desc = (String) pdf.get("description");
                    textBody.append(desc).append(": ").append(url).append("\n");
                    htmlBody.append("<p><a href='").append(url).append("'>Download ").append(desc).append("</a></p>");
                }
            }
        }
        
        textBody.append("\nThank you for using TapData!");
        htmlBody.append("<br/><p>Thank you for using TapData!</p>");
        
        try {
            emailService.sendHtml(tx.getEmail(), subject, textBody.toString(), htmlBody.toString());
            log.info("Delivery email sent to {}", tx.getEmail());
        } catch (Exception e) {
            log.error("Failed to send delivery email to {}: {}", tx.getEmail(), e.getMessage());
        }
    }
}

package com.space.space_bundle.service;

import com.space.space_bundle.dto.checkerport.*;
import com.space.space_bundle.entity.ResultCheckerPricing;
import com.space.space_bundle.entity.ResultsTransaction;
import com.space.space_bundle.entity.ResultsTransactionType;
import com.space.space_bundle.entity.ServiceStatus;
import com.space.space_bundle.repository.ResultCheckerPricingRepository;
import com.space.space_bundle.repository.ResultsTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultsCheckerService {

    private final CheckerPortClient checkerPortClient;
    private final ResultsTransactionRepository transactionRepository;
    private final ResultCheckerPricingRepository pricingRepository;

    public ResultsTransaction buyVoucher(CheckerPortVoucherRequest request, String userId) {
        // Validate request
        if (request.getQty() == null || request.getQty() < 1 || request.getQty() > 200) {
            throw new IllegalArgumentException("Quantity must be between 1 and 200");
        }

        // We can fetch price from DB or trust the request
        CheckerPortResponse<Map<String, Object>> response = checkerPortClient.buyVoucher(request);

        ResultsTransaction transaction = ResultsTransaction.builder()
                .referenceId(request.getReferenceId())
                .userId(userId)
                .type(ResultsTransactionType.VOUCHER)
                .status(ServiceStatus.PENDING)
                .phoneNumber(request.getPhoneNumber())
                .price(request.getPrice())
                .qty(request.getQty())
                .platform(request.getPlatform())
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
                .type(tType)
                .status(ServiceStatus.PENDING)
                .phoneNumber(request.getPhoneNumber())
                .price(request.getPrice())
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
        return transactionRepository.findByReferenceId(referenceId);
    }

    public void updatePricingFromApi() {
        String[] services = {"VoucherPricePlatformWaecNew", "VoucherPricePlatformWaecOld",
                "ArcPriceWrcBeceSchool", "ArcPriceWrcWassceSchool", "ArcPriceWrcWasscePrivate", "ArcPriceSpr"};

        for (String s : services) {
            try {
                CheckerPortResponse<Map<String, Object>> resp = checkerPortClient.getServicePrice(s);
                if ("SUCCESS".equalsIgnoreCase(resp.getStatus()) && resp.getData() != null) {
                    Object priceObj = resp.getData().get("price");
                    if (priceObj != null) {
                        BigDecimal amount = new BigDecimal(priceObj.toString());
                        ResultCheckerPricing pricing = ResultCheckerPricing.builder()
                                .serviceName(s)
                                .amount(amount)
                                .updatedAt(LocalDateTime.now())
                                .build();
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
}

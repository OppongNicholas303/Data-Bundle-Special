package com.space.space_bundle.service;

import com.space.space_bundle.entity.Order;
import com.space.space_bundle.dto.BotPurchaseRequest;
import com.space.space_bundle.dto.BotPurchaseResponse;
import com.space.space_bundle.dto.BotPurchaseResponseRandy;
import com.space.space_bundle.dto.ExternalOrderStatusDto;
import com.space.space_bundle.dto.LessDataOrderRequest;
import com.space.space_bundle.dto.LessDataOrderResponse;
import com.space.space_bundle.dto.MyDataGigsStatusResponse;
import com.space.space_bundle.dto.RandyStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationService {

    private final WebClient webClient;

    // ── MyDataGigs config ──────────────────────────────────────────────────
    @Value("${bot.api.url}")
    private String botUrl;

    @Value("${bot.api.token}")
    private String botToken;

    // ── Randy config ───────────────────────────────────────────────────────
    @Value("${bot.api.urlRandy}")
    private String botUrlRandy;

    @Value("${bot.api.tokenRandy}")
    private String botTokenRandy;

    // ── LessData config ────────────────────────────────────────────────────
    @Value("${lessdata.base-url:https://api.lessbundle.com}")
    private String lessDataBaseUrl;

    @Value("${lessdata.api-key:YOUR_LESSDATA_API_KEY}")
    private String lessDataApiKey;

    @Value("${lessdata.callback-url:}")
    private String lessDataCallbackUrl;

    // ── MyDataGigs ─────────────────────────────────────────────────────────

    public String buy(Order order) {
        String network;
        if (order.getNetwork().equalsIgnoreCase("VODAFONE")) {
            network = "telecel";
        } else if (order.getNetwork().equalsIgnoreCase("AIRTELTIGO")) {
            network = "ISHARE".equalsIgnoreCase(order.getBundleType()) ? "at_ishare" : "at_bigdata";
        } else {
            network = order.getNetwork().toLowerCase();
        }

        BotPurchaseRequest request = BotPurchaseRequest.builder()
                .network(network)
                .beneficiary(order.getPhoneNumber())
                .dataBundlePackages(extractPackageId(order.getBundleCode()))
                .build();

        try {
            log.info("Sending request to bot: {}", request);
            BotPurchaseResponse response = webClient.post()
                    .uri(botUrl)
                    .header("Authorization", "Bearer " + botToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BotPurchaseResponse.class)
                    .block();

            if (response == null) throw new RuntimeException("No response from bot");
            if (!"success".equalsIgnoreCase(response.getStatus())) {
                String errMsg = response.getMessage() != null ? response.getMessage() : "Invalid bot response";
                throw new RuntimeException(errMsg);
            }
            if (response.getOrderId() == null) throw new RuntimeException("No order_id in bot response");
            return String.valueOf(response.getOrderId());

        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            log.error("Bot API network timeout: {}", e.getMessage(), e);
            throw new com.space.space_bundle.exception.ProviderTimeoutException("Provider network timeout: " + e.getMessage(), e);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                log.error("Bot API 5xx error: {}", e.getMessage(), e);
                throw new com.space.space_bundle.exception.ProviderTimeoutException("Provider 5xx error: " + e.getMessage(), e);
            }
            log.error("Bot API 4xx failed: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (Exception e) {
            log.error("Bot API failed: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public MyDataGigsStatusResponse checkMyDataGigsStatus(String orderId) {
        try {
            String statusUrl = "https://mydatagigs.com/wp-json/custom/v1/order-status?order_id=" + orderId;
            MyDataGigsStatusResponse response = webClient.get()
                    .uri(statusUrl)
                    .header("Authorization", "Bearer " + botToken)
                    .retrieve()
                    .bodyToMono(MyDataGigsStatusResponse.class)
                    .block();
            if (response == null || !"success".equalsIgnoreCase(response.getStatus()))
                throw new RuntimeException("Invalid MyDataGigs status response");
            return response;
        } catch (Exception e) {
            log.error("MyDataGigs status check failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check MyDataGigs order status", e);
        }
    }

    // ── Randy ──────────────────────────────────────────────────────────────

    public String buyFromRandy(Order order) {
        BotPurchaseRequest request = BotPurchaseRequest.builder()
                .package_id(Integer.parseInt(order.getPackageId()))
                .customer_phone(order.getPhoneNumber())
                .build();
        try {
            log.info("Sending request to Randy bot: {}", request);
            BotPurchaseResponseRandy body = webClient.post()
                    .uri(botUrlRandy + "/external/orders")
                    .header("X-API-Key", botTokenRandy)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BotPurchaseResponseRandy.class)
                    .block();

            if (body == null || !body.success() || body.order() == null || body.order().id() == null)
                throw new RuntimeException(body != null ? body.message() : "No response");
            return String.valueOf(body.order().order_number());

        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            log.error("Randy bot network timeout: {}", e.getMessage(), e);
            throw new com.space.space_bundle.exception.ProviderTimeoutException("Randy bot network timeout: " + e.getMessage(), e);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                log.error("Randy bot 5xx error: {}", e.getMessage(), e);
                throw new com.space.space_bundle.exception.ProviderTimeoutException("Randy bot 5xx error: " + e.getMessage(), e);
            }
            log.error("Randy bot 4xx failed: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (Exception e) {
            log.error("Randy bot failed: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String buyFromRandyMashup(Order order) {
        // Same endpoint as buyFromRandy — reuse
        return buyFromRandy(order);
    }

    public BotPurchaseResponseRandy checkStatus(String orderNumber) {
        try {
            BotPurchaseResponseRandy response = webClient.get()
                    .uri(botUrlRandy + "/external/orders/status?order_number=" + orderNumber)
                    .header("X-API-Key", botTokenRandy)
                    .retrieve()
                    .bodyToMono(BotPurchaseResponseRandy.class)
                    .block();
            if (response == null || !response.success() || response.order() == null)
                throw new RuntimeException("Invalid status response");
            return response;
        } catch (Exception e) {
            log.error("Status check failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check order status", e);
        }
    }

    // ── LessData ───────────────────────────────────────────────────────────

    public String  buyFromLessData(Order order) {
        String network = resolveLessDataNetwork(order.getNetwork(), order.getBundleType());
        int sizeGb = extractSizeGb(order.getBundleCode());

        LessDataOrderRequest request = LessDataOrderRequest.builder()
                .phone(order.getPhoneNumber())
                .size(sizeGb)
                .network(network)
                .callback(lessDataCallbackUrl != null && !lessDataCallbackUrl.isBlank()
                        ? lessDataCallbackUrl : null)
                .build();

        try {
            log.info("[LESSDATA] Sending order: phone={}, size={}GB, network={}",
                    order.getPhoneNumber(), sizeGb, network);

            LessDataOrderResponse response = webClient.post()
                    .uri(lessDataBaseUrl + "/api/order")
                    .header("X-API-Key", lessDataApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LessDataOrderResponse.class)
                    .block();

            if (response == null)
                throw new RuntimeException("No response from LessData");
            if (!response.isStatus() || response.getPayload() == null)
                throw new RuntimeException("LessData error: " + response.getMessage());

            String orderId = response.getPayload().getId();
            log.info("[LESSDATA] Order created: id={}, code={}, status={}",
                    orderId, response.getPayload().getOrderCode(), response.getPayload().getStatus());
            return orderId;

        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            log.error("[LESSDATA] Network timeout: {}", e.getMessage(), e);
            throw new com.space.space_bundle.exception.ProviderTimeoutException("LessData network timeout: " + e.getMessage(), e);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                log.error("[LESSDATA] 5xx error: {}", e.getMessage(), e);
                throw new com.space.space_bundle.exception.ProviderTimeoutException("LessData 5xx error: " + e.getMessage(), e);
            }
            log.error("[LESSDATA] 4xx error: {}", e.getMessage(), e);
            throw new RuntimeException("LessData error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[LESSDATA] Failed: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public LessDataOrderResponse checkLessDataStatus(String lessDataOrderId) {
        try {
            log.info("[LESSDATA] Checking status: orderId={}", lessDataOrderId);
            LessDataOrderResponse response = webClient.get()
                    .uri(lessDataBaseUrl + "/api/order/" + lessDataOrderId)
                    .header("X-API-Key", lessDataApiKey)
                    .retrieve()
                    .bodyToMono(LessDataOrderResponse.class)
                    .block();
            if (response == null || !response.isStatus())
                throw new RuntimeException("Invalid LessData status response");
            return response;
        } catch (Exception e) {
            log.error("[LESSDATA] Status check failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check LessData order status", e);
        }
    }

    // ── External status dispatcher ─────────────────────────────────────────

    public ExternalOrderStatusDto checkExternalStatus(Order order) {
        if ("randy".equalsIgnoreCase(order.getByFrom())) {
            try {
                RandyStatusResponse response = webClient.get()
                        .uri(botUrlRandy + "/external/orders/status?order_number=" + order.getProviderReference())
                        .header("X-API-Key", botTokenRandy)
                        .retrieve()
                        .bodyToMono(RandyStatusResponse.class)
                        .block();
                if (response != null && response.isSuccess() && response.getOrder() != null) {
                    String status = response.getOrder().getStatus();
                    if ("completed".equalsIgnoreCase(status)) status = "Delivered";
                    return ExternalOrderStatusDto.builder()
                            .provider("randy").status(status)
                            .providerOrderId(response.getOrder().getOrder_number())
                            .amount(response.getOrder().getCost_price())
                            .build();
                }
            } catch (Exception e) {
                log.error("Failed to check status from Randy", e);
            }

        } else if ("lessdata".equalsIgnoreCase(order.getByFrom())) {
            try {
                LessDataOrderResponse response = checkLessDataStatus(order.getProviderReference());
                if (response != null && response.isStatus() && response.getPayload() != null) {
                    String status = response.getPayload().getStatus();
                    if ("COMPLETED".equalsIgnoreCase(status)) status = "Delivered";
                    return ExternalOrderStatusDto.builder()
                            .provider("lessdata").status(status)
                            .providerOrderId(response.getPayload().getId())
                            .amount(response.getPayload().getPrice() != null
                                    ? String.valueOf(response.getPayload().getPrice()) : "0")
                            .build();
                }
            } catch (Exception e) {
                log.error("Failed to check status from LessData", e);
            }

        } else {
            try {
                MyDataGigsStatusResponse response = webClient.get()
                        .uri("https://mydatagigs.com/wp-json/custom/v1/order-status?order_id=" + order.getProviderReference())
                        .header("Authorization", "Bearer " + botToken)
                        .retrieve()
                        .bodyToMono(MyDataGigsStatusResponse.class)
                        .block();
                if (response != null && "success".equalsIgnoreCase(response.getStatus())) {
                    return ExternalOrderStatusDto.builder()
                            .provider("mydatagigs").status(response.getOrder_status())
                            .providerOrderId(String.valueOf(response.getOrder_id()))
                            .amount(String.valueOf(response.getAmount()))
                            .build();
                }
            } catch (Exception e) {
                log.error("Failed to check status from MyDataGigs", e);
            }
        }

        return ExternalOrderStatusDto.builder()
                .provider(order.getByFrom()).status("Unknown")
                .providerOrderId(order.getProviderReference()).amount("0.00")
                .build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String resolveLessDataNetwork(String network, String bundleType) {
        if (network == null) return "MTN";
        return switch (network.toUpperCase()) {
            case "MTN"             -> "MTN";
            case "TELECEL",
                 "VODAFONE"        -> "TELECEL";
            case "AIRTELTIGO"      -> "ISHARE".equalsIgnoreCase(bundleType)
                                        ? "AIRTELTIGO_ISHARE"
                                        : "AIRTELTIGO_BIGTIME";
            default                -> network.toUpperCase();
        };
    }

    private int extractSizeGb(String bundleCode) {
        if (bundleCode == null) return 1;
        String upper = bundleCode.toUpperCase().trim();
        try {
            if (upper.endsWith("GB")) {
                double gb = Double.parseDouble(upper.replace("GB", "").trim());
                return Math.max(1, (int) Math.round(gb));
            } else if (upper.endsWith("MB")) {
                double mb = Double.parseDouble(upper.replace("MB", "").trim());
                return Math.max(1, (int) Math.ceil(mb / 1024.0));
            }
        } catch (NumberFormatException ignored) {}
        return 1;
    }

    private Integer extractPackageId(String bundleCode) {
        return switch (bundleCode.toUpperCase()) {
            case "1GB" -> 1;   case "2GB" -> 2;   case "3GB" -> 3;
            case "4GB" -> 4;   case "5GB" -> 5;   case "6GB" -> 6;
            case "7GB" -> 7;   case "8GB" -> 8;   case "10GB" -> 10;
            case "12GB" -> 12; case "15GB" -> 15; case "20GB" -> 20;
            case "25GB" -> 25; case "30GB" -> 30; case "40GB" -> 40;
            case "50GB" -> 50; case "100GB" -> 100;
            default -> 0;
        };
    }
}

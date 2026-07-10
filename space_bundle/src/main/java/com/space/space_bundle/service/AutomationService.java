package com.space.space_bundle.service;

import com.space.space_bundle.entity.Order;
import com.space.space_bundle.dto.BotPurchaseRequest;
import com.space.space_bundle.dto.BotPurchaseResponse;
import com.space.space_bundle.dto.BotPurchaseResponseRandy;
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

    @Value("${bot.api.url}")
    private String botUrl;

    @Value("${bot.api.urlRandy}")
    private String botUrlRandy;

    @Value("${bot.api.token}")
    private String botToken;

    @Value("${bot.api.tokenRandy}")
    private String botTokenRandy;

    public String buy(Order order) {
        String network = order.getNetwork().equalsIgnoreCase("VODAFONE")
                ? "telecel" : order.getNetwork().toLowerCase();

        BotPurchaseRequest request = BotPurchaseRequest.builder()
                .network(network)
                .beneficiary(order.getPhoneNumber())
                .dataBundlePackages(extractPackageId(order.getBundleCode()))
                .build();

        try {
            BotPurchaseResponse response = webClient.post()
                    .uri(botUrl)
                    .header("Authorization", "Bearer " + botToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BotPurchaseResponse.class)
                    .block();

            if (response == null) throw new RuntimeException("No response from bot");
            if (response.getCode() != null) throw new RuntimeException(response.getMessage());
            if (!"success".equalsIgnoreCase(response.getStatus()) || response.getOrderId() == null)
                throw new RuntimeException("Invalid bot response");

            return String.valueOf(response.getOrderId());
        } catch (Exception e) {
            log.error("Bot API failed: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

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
        } catch (Exception e) {
            log.error("Randy bot failed: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String buyFromRandyMashup(Order order) {
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
        } catch (Exception e) {
            log.error("Randy bot failed: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
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

    private Integer extractPackageId(String bundleCode) {
        return switch (bundleCode.toUpperCase()) {
            case "1GB" -> 1; case "2GB" -> 2; case "3GB" -> 3;
            case "4GB" -> 4; case "5GB" -> 5; case "6GB" -> 6;
            case "7GB" -> 7; case "8GB" -> 8; case "10GB" -> 10;
            case "12GB" -> 12; case "15GB" -> 15; case "20GB" -> 20;
            case "25GB" -> 25; case "30GB" -> 30; case "40GB" -> 40;
            case "50GB" -> 50; case "100GB" -> 100;
            default -> 0;
        };
    }
}

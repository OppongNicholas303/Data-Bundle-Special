package com.space.space_bundle.out.automation;

import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.core.port.out.AutomationPort;
import com.space.space_bundle.core.port.out.OrderRepositoryPort;
import com.space.space_bundle.core.port.out.dto.PackageDto;
import com.space.space_bundle.core.port.out.dto.PackageResponseDto;
import com.space.space_bundle.out.automation.dto.BotPurchaseRequest;
import com.space.space_bundle.out.automation.dto.BotPurchaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutomationAdapter implements AutomationPort {

    @Value("${bot.api.url:https://myspaceserver.com/api/}")
    private String botApiUrl;
    
    @Value("${bot.api.token:sk_4975646ef9cc4a5a2bcfd62c0f60f8a0a713de8114df7c47965b2703a9d555f7}")
    private String botApiToken;

    private final WebClient webClient;

    private final OrderRepositoryPort orderRepositoryPort;


    @Override
    public String buyDataBundle(Order order) {
        BotPurchaseRequest request = BotPurchaseRequest.builder()
                .package_id(order.getPackage_id())
                .customer_phone(order.getPhoneNumber())
                .build();

        log.info("Sending order to bot API: {}", botApiUrl);
        log.info("Request body: {}", request);

        try {

            BotPurchaseResponse body = webClient.post()
                    .uri(botApiUrl + "/external/orders")
                    .header("X-API-Key", botApiToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BotPurchaseResponse.class)
                    .block();  // blocking because your method returns String

            if (body == null) {
                throw new RuntimeException("No response from bot");
            }

            if (!body.success()
                    || body.order() == null
                    || body.order().id() == null) {

                throw new RuntimeException(body.message());
            }

            log.info("Bot purchase successful: orderId={}", body.order().order_number());
            return String.valueOf(body.order().order_number());

        } catch (Exception e) {
            log.error("Bot API call failed", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public BotPurchaseResponse checkOrderStatus(String orderNumber) {

        try {
            BotPurchaseResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(botApiUrl + "/external/orders/status")
                            .queryParam("order_number", orderNumber)
                            .build())
                    .header("X-API-Key", "Bearer " + botApiToken)
                    .retrieve()
                    .bodyToMono(BotPurchaseResponse.class)
                    .block();

            if (response == null || !response.success() || response.order() == null) {
                throw new RuntimeException("Invalid status response from bot");
            }

            log.info("Order {} status: {}", orderNumber, response.order().status());

            return response;

        } catch (Exception e) {
            log.error("Failed to check order status", e);
            throw new RuntimeException("Failed to check order status", e);
        }
    }

    @Override
    public List<PackageDto> getBundlePackage() {
        try {
            PackageResponseDto response = webClient.get()
                    .uri(botApiUrl + "/external/packages")
                    .header("X-API-Key",  botApiToken)
                    .retrieve()
                    .bodyToMono(PackageResponseDto.class)
                    .block();

            if (response == null || !response.success() || response.packages() == null) {
                throw new RuntimeException("Invalid packages response from bot");
            }

            log.info("Fetched {} packages from bot", response.packages().size());
            return response.packages();

        } catch (Exception e) {
            log.error("Failed to fetch packages", e);
            throw new RuntimeException("Failed to fetch packages", e);
        }
    }
}
package com.space.space_bundle.out.automation;

import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.core.port.out.AutomationPort;
import com.space.space_bundle.out.automation.dto.BotPurchaseRequest;
import com.space.space_bundle.out.automation.dto.BotPurchaseResponse;
import com.space.space_bundle.out.automation.dto.BotPurchaseResponseRandy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutomationAdapter implements AutomationPort {


    private final WebClient webClient;

    @Value("${bot.api.url:https://mydatagigs.com/wp-json/custom/v1/place-order}")
    private String botApiUrl;

    @Value("${bot.api.urlRandy}")
    private String botApiUrlRandy;
    
    @Value("${bot.api.token}")
    private String botApiToken;

    @Value("${bot.api.tokenRandy}")
    private String botApiTokenRandy;

    @Override
    public String buyDataBundle(Order order) {

        String network = order.getNetwork().equalsIgnoreCase("VODAFONE")
                ? "telecel"
                : order.getNetwork().toLowerCase();

        Integer packageId = extractPackageId(order.getBundleCode());

        BotPurchaseRequest request = BotPurchaseRequest.builder()
                .network(network)
                .beneficiary(order.getPhoneNumber())
                .dataBundlePackages(packageId)
                .build();

        log.info("Sending order to bot API: {}", botApiUrl);
        log.info("Request body: {}", request);

        try {

            BotPurchaseResponse response = webClient.post()
                    .uri(botApiUrl)
                    .header("Authorization", "Bearer " + botApiToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BotPurchaseResponse.class)
                    .block(); // blocking since your service is synchronous

            log.info("Bot API response: {}", response);

            if (response == null) {
                throw new RuntimeException("No response from bot");
            }

            if (response.getCode() != null) {
                throw new RuntimeException(response.getMessage());
            }

            if (!"success".equalsIgnoreCase(response.getStatus()) || response.getOrderId() == null) {
                throw new RuntimeException("Invalid response from bot");
            }

            log.info("Bot purchase successful: orderId={}", response.getOrderId());

            return String.valueOf(response.getOrderId());

        } catch (Exception e) {
            log.error("Bot API call failed: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    @Override
    public String buyDataBundleFromRandy(Order order) {
        BotPurchaseRequest request = BotPurchaseRequest.builder()
                .package_id(order.getPackage_id())
                .customer_phone(order.getPhoneNumber())
                .build();

        log.info("Sending order to bot API: {}", botApiUrl);
        log.info("Request body: {}", request);

        try {

            BotPurchaseResponseRandy body = webClient.post()
                    .uri(botApiUrlRandy + "/external/orders")
                    .header("X-API-Key", botApiTokenRandy)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BotPurchaseResponseRandy.class)
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


    private Integer extractPackageId(String bundleCode) {
        if (bundleCode == null) return null;
        switch (bundleCode.toUpperCase()) {
            case "1GB": return 20;
            case "2GB": return 21;
            case "3GB": return 23;
            default: return null;
        }
    }
}
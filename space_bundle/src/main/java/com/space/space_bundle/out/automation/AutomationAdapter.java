package com.space.space_bundle.out.automation;

import com.space.space_bundle.core.entities.Order;
import com.space.space_bundle.core.port.out.AutomationPort;
import com.space.space_bundle.out.automation.dto.BotPurchaseRequest;
import com.space.space_bundle.out.automation.dto.BotPurchaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutomationAdapter implements AutomationPort {

    private final RestTemplate restTemplate;
    
    @Value("${bot.api.url:https://mydatagigs.com/wp-json/custom/v1/place-order}")
    private String botApiUrl;
    
    @Value("${bot.api.token:tera_live_d7a798a4790c08e51366d0580276835b}")
    private String botApiToken;

    @Override
    public String buyDataBundle(Order order) {
        String network = order.getNetwork().equalsIgnoreCase("VODAFONE") 
                ? "telecel" 
                : order.getNetwork().toLowerCase();
        
        Integer packageId = extractPackageId(order.getBundleCode());

        System.out.println(network + " " + order.getBundleCode());
        BotPurchaseRequest request = BotPurchaseRequest.builder()
                .network(network)
                .beneficiary(order.getPhoneNumber())
                .dataBundlePackages(packageId)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + botApiToken);
        headers.set("Content-Type", "application/json");
        
        HttpEntity<BotPurchaseRequest> entity = new HttpEntity<>(request, headers);
        
        log.info("Sending order to bot API: {}", botApiUrl);
        log.info("Request body: network={}, beneficiary={}, pa_data-bundle-packages={}", 
                network, order.getPhoneNumber(), packageId);
        log.info("Full request object: {}", request);
        
        try {
            ResponseEntity<BotPurchaseResponse> response = restTemplate.exchange(
                    botApiUrl,
                    HttpMethod.POST,
                    entity,
                    BotPurchaseResponse.class
            );
            
            BotPurchaseResponse body = response.getBody();
            log.info("Bot API response status: {}", response.getStatusCode());
            log.info("Bot API response body: {}", body);
            
            if (body == null) {
                throw new RuntimeException("No response from bot");
            }
            
            if (body.getCode() != null) {
                throw new RuntimeException(body.getMessage());
            }
            
            if (!"success".equalsIgnoreCase(body.getStatus()) || body.getOrderId() == null) {
                throw new RuntimeException("Invalid response from bot");
            }
            
            log.info("Bot purchase successful: orderId={}", body.getOrderId());
            return String.valueOf(body.getOrderId());
            
        } catch (Exception e) {
            log.error("Bot API call failed: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    private Integer extractPackageId(String bundleCode) {
        switch (bundleCode.toUpperCase()) {
            case "1GB": return 1;
            case "2GB": return 2;
            case "3GB": return 3;
            case "4GB": return 4;
            case "5GB": return 5;
            case "6GB": return 6;
            case "7GB": return 7;
            case "8GB": return 8;
            case "12GB": return 12;
            case "15GB": return 15;
            case "20GB": return 20;
            case "25GB": return 25;
            case "30GB": return 30;
            case "40GB": return 40;
            case "50GB": return 50;
            case "100GB": return 100;
            default: return 0;
        }
    }
}
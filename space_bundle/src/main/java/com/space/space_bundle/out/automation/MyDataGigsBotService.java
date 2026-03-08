package com.space.space_bundle.out.automation;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Service
public class MyDataGigsBotService {

    @Value("${bot.login.url:https://mydatagigs.com/login/}")
    private String loginUrl;

    @Value("${bot.login.username:nicholas.oppong2623@gmail.com}")
    private String username;

    @Value("${bot.login.password:Vida0243911336@}")
    private String password;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;

    public record OrderStatus(
            String orderId,
            String status,
            String beneficiary,
            String dataPackage,
            String amount,
            String date
    ) {}

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing Playwright...");
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            // Try to load context from state if it exists
            if (java.nio.file.Files.exists(Paths.get("state.json"))) {
                context = browser.newContext(new Browser.NewContextOptions()
                        .setStorageStatePath(Paths.get("state.json")));
                log.info("Context initialized with saved state.");
            } else {
                context = browser.newContext();
                log.info("Fresh context initialized.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Playwright: {}", e.getMessage(), e);
        }
    }

    public synchronized OrderStatus trackOrder(String phoneNumber) {
        log.info("Tracking order fast via direct POST for: {}", phoneNumber);
        try {
            // Direct POST request to the tracking URL
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect("https://mydatagigs.com/track-order/")
                    .data("beneficiary_phone", phoneNumber)
                    .post();

            // Parse the first order card from the response HTML
            org.jsoup.nodes.Element firstCard = doc.selectFirst(".order-card");
            if (firstCard == null) {
                log.warn("No order found for number: {}", phoneNumber);
                return null;
            }

            String orderId = firstCard.select(".order-row:nth-child(1) .order-value").text();
            String status = firstCard.select(".order-status").text();
            String beneficiary = firstCard.select(".order-row:nth-child(3) .order-value").text();
            String dataPackage = firstCard.select(".order-row:nth-child(4) .order-value").text();
            String amount = firstCard.select(".order-row:nth-child(5) .order-value").text();
            String date = firstCard.select(".order-row:nth-child(6) .order-value").text();

            log.info("Found order: {} for number: {}", orderId, phoneNumber);
            return new OrderStatus(orderId, status, beneficiary, dataPackage, amount, date);

        } catch (Exception e) {
            log.error("Fast order tracking failed, falling back to browser: {}", e.getMessage());
            return trackOrderWithBrowser(phoneNumber);
        }
    }

    private synchronized OrderStatus trackOrderWithBrowser(String phoneNumber) {
        if (context == null) init();
        
        try (Page page = context.newPage()) {
            page.route("**/*.{png,jpg,jpeg,gif,css,svg,woff,woff2}", Route::abort);
            page.navigate("https://mydatagigs.com/track-order/", 
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            
            page.fill("input[name='beneficiary_phone']", phoneNumber);
            page.click("button[type='submit']");
            
            page.waitForSelector(".order-card", new Page.WaitForSelectorOptions().setTimeout(5000));
            
            ElementHandle firstCard = page.querySelector(".order-card");
            if (firstCard == null) return null;

            String orderId = firstCard.querySelector(".order-row:nth-child(1) .order-value").innerText();
            String status = firstCard.querySelector(".order-status").innerText();
            String beneficiary = firstCard.querySelector(".order-row:nth-child(3) .order-value").innerText();
            String dataPackage = firstCard.querySelector(".order-row:nth-child(4) .order-value").innerText();
            String amount = firstCard.querySelector(".order-row:nth-child(5) .order-value").innerText();
            String date = firstCard.querySelector(".order-row:nth-child(6) .order-value").innerText();

            return new OrderStatus(orderId, status, beneficiary, dataPackage, amount, date);
        } catch (Exception e) {
            log.error("Browser order tracking also failed: {}", e.getMessage());
            return null;
        }
    }

    public synchronized void login() {
        if (context == null) {
            init();
        }
        
        try (Page page = context.newPage()) {
            performLogin(page);
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to login to MyDataGigs", e);
        }
    }

    private void performLogin(Page page) {
        log.info("Navigating to login page: {}", loginUrl);
        page.navigate(loginUrl);

        log.info("Filling login form for user: {}", username);
        page.fill("input#wppb_user_login", username);
        page.fill("input#wppb_user_pass", password);
        
        log.info("Clicking login button");
        page.click("input#wppb-submit");

        // Wait for navigation or a specific element to confirm login
        page.waitForURL(url -> !url.contains("/login/"), new Page.WaitForURLOptions().setTimeout(15000));
        
        log.info("Login successful. Current URL: {}", page.url());
        
        log.info("Navigating to track-order page...");
        page.navigate("https://mydatagigs.com/track-order/");
        log.info("Successfully navigated to: {}", page.url());
        
        // Save storage state to reuse session
        context.storageState(new BrowserContext.StorageStateOptions().setPath(Paths.get("state.json")));
        log.info("Session state saved to state.json");
    }

    @PreDestroy
    public void cleanup() {
        log.info("Closing Playwright resources...");
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}

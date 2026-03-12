package com.space.space_bundle;

import com.space.space_bundle.out.email.EmailAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Test Application for Email Exception Handling
 * Demonstrates email sending when exceptions occur in webhook processing
 */
public class EmailExceptionTestApp {
    
    private static final String TEST_EMAIL = "nictech23@gmail.com";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║    Email Exception Handling Test Suite - Space Bundle         ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
        
        // Start Spring Application Context
        ApplicationContext context = SpringApplication.run(SpaceBundleApplication.class, args);
        
        // Get the EmailAdapter bean
        EmailAdapter emailAdapter = context.getBean(EmailAdapter.class);
        JavaMailSender mailSender = context.getBean(JavaMailSender.class);
        
        // Run test scenarios
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 1: Webhook Processing Exception");
        System.out.println("=".repeat(70));
        testWebhookProcessingException(emailAdapter);
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 2: Signature Verification Failure");
        System.out.println("=".repeat(70));
        testSignatureVerificationFailure(emailAdapter);
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 3: Order Processing Exception");
        System.out.println("=".repeat(70));
        testOrderProcessingException(emailAdapter);
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 4: Wallet Top-up Exception");
        System.out.println("=".repeat(70));
        testTopUpException(emailAdapter);
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 5: Multiple Concurrent Exceptions");
        System.out.println("=".repeat(70));
        testMultipleExceptions(emailAdapter);
        
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     All Tests Completed!                       ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
    }
    
    /**
     * TEST 1: Simulates webhook processing exception
     */
    private static void testWebhookProcessingException(EmailAdapter emailAdapter) {
        String testName = "Webhook Processing Exception";
        System.out.println("\n[" + LocalDateTime.now().format(formatter) + "] Starting: " + testName);
        
        try {
            String subject = "Webhook Controller Exception";
            String body = "Exception in handlePaystackWebhook endpoint:\n" +
                         "java.lang.NullPointerException: Payload is null\n" +
                         "at com.space.space_bundle.core.services.PaymentWebhookService.processPaystackWebhook(PaymentWebhookService.java:45)\n" +
                         "at com.space.space_bundle.in.web.controller.WebhookController.handlePaystackWebhook(WebhookController.java:50)";
            
            System.out.println("  → Sending email to: " + TEST_EMAIL);
            System.out.println("  → Subject: " + subject);
            System.out.println("  → Body: " + body.substring(0, Math.min(80, body.length())) + "...");
            
            emailAdapter.sendEmail(TEST_EMAIL, subject, body);
            
            System.out.println("  ✓ Email sent successfully!");
            
        } catch (Exception e) {
            System.out.println("  ✗ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * TEST 2: Simulates signature verification failure
     */
    private static void testSignatureVerificationFailure(EmailAdapter emailAdapter) {
        String testName = "Signature Verification Failure";
        System.out.println("\n[" + LocalDateTime.now().format(formatter) + "] Starting: " + testName);
        
        try {
            String subject = "Webhook Signature Verification Failed";
            String body = "Exception during Paystack signature verification:\n" +
                         "java.security.SignatureException: HMAC signature verification failed\n" +
                         "Expected: a1b2c3d4e5f6...\n" +
                         "Received: x1y2z3...\n" +
                         "at com.space.space_bundle.in.web.controller.WebhookController.verifySignature(WebhookController.java:65)";
            
            System.out.println("  → Sending email to: " + TEST_EMAIL);
            System.out.println("  → Subject: " + subject);
            System.out.println("  → Event: Signature mismatch detected");
            System.out.println("  → Timestamp: " + LocalDateTime.now().format(formatter));
            
            emailAdapter.sendEmail(TEST_EMAIL, subject, body);
            
            System.out.println("  ✓ Email sent successfully!");
            
        } catch (Exception e) {
            System.out.println("  ✗ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * TEST 3: Simulates order processing exception
     */
    private static void testOrderProcessingException(EmailAdapter emailAdapter) {
        String testName = "Order Processing Exception";
        System.out.println("\n[" + LocalDateTime.now().format(formatter) + "] Starting: " + testName);
        
        try {
            String orderId = "ORDER_98765";
            String subject = "Order Exception: " + orderId;
            String body = "Bot processing failed for order: " + orderId + "\n" +
                         "Error: Connection timeout to MyDataGigs service after 30 seconds\n" +
                         "Stack Trace:\n" +
                         "java.net.SocketTimeoutException: Read timed out\n" +
                         "at java.net.SocketInputStream.socketRead0(SocketInputStream.java:115)\n" +
                         "Order Amount: GHS 20.00\n" +
                         "Bundle: MTN 5GB\n" +
                         "User Phone: +233123456789";
            
            System.out.println("  → Sending email to: " + TEST_EMAIL);
            System.out.println("  → Subject: " + subject);
            System.out.println("  → Order ID: " + orderId);
            System.out.println("  → Error Type: Connection Timeout");
            System.out.println("  → Severity: HIGH");
            
            emailAdapter.sendEmail(TEST_EMAIL, subject, body);
            
            System.out.println("  ✓ Email sent successfully!");
            System.out.println("  ⚠ Admin will be notified to retry order processing");
            
        } catch (Exception e) {
            System.out.println("  ✗ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * TEST 4: Simulates wallet top-up exception
     */
    private static void testTopUpException(EmailAdapter emailAdapter) {
        String testName = "Wallet Top-up Exception";
        System.out.println("\n[" + LocalDateTime.now().format(formatter) + "] Starting: " + testName);
        
        try {
            String reference = "TOPUP_54321";
            String subject = "Top-up Exception: " + reference;
            String body = "Failed to process top-up.\nError: Wallet update failed - User account locked\n" +
                         "Reference: " + reference + "\n" +
                         "Transaction ID: TXN_7890\n" +
                         "Amount: GHS 50.00\n" +
                         "Status: FAILED\n" +
                         "Reason: Account security check required";
            
            System.out.println("  → Sending email to: " + TEST_EMAIL);
            System.out.println("  → Subject: " + subject);
            System.out.println("  → Reference: " + reference);
            System.out.println("  → Amount: GHS 50.00");
            System.out.println("  → Issue: Account Locked");
            
            emailAdapter.sendEmail(TEST_EMAIL, subject, body);
            
            System.out.println("  ✓ Email sent successfully!");
            System.out.println("  ⚠ User will need to unlock account before retrying");
            
        } catch (Exception e) {
            System.out.println("  ✗ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * TEST 5: Multiple concurrent exceptions
     */
    private static void testMultipleExceptions(EmailAdapter emailAdapter) {
        String testName = "Multiple Concurrent Exceptions";
        System.out.println("\n[" + LocalDateTime.now().format(formatter) + "] Starting: " + testName);
        
        // Create multiple threads to send emails concurrently
        Thread[] threads = new Thread[3];
        
        threads[0] = new Thread(() -> {
            try {
                System.out.println("  [Thread-1] Sending webhook exception notification...");
                emailAdapter.sendEmail(TEST_EMAIL, 
                    "Webhook Exception #1",
                    "Database connection failed during webhook processing");
                System.out.println("    ✓ Exception #1 notification sent");
            } catch (Exception e) {
                System.out.println("    ✗ Failed to send exception #1: " + e.getMessage());
            }
        });
        
        threads[1] = new Thread(() -> {
            try {
                System.out.println("  [Thread-2] Sending order exception notification...");
                emailAdapter.sendEmail(TEST_EMAIL,
                    "Order Exception #2",
                    "Bot service unreachable for order: ORDER_11111");
                System.out.println("    ✓ Exception #2 notification sent");
            } catch (Exception e) {
                System.out.println("    ✗ Failed to send exception #2: " + e.getMessage());
            }
        });
        
        threads[2] = new Thread(() -> {
            try {
                System.out.println("  [Thread-3] Sending timeout notification...");
                emailAdapter.sendEmail(TEST_EMAIL,
                    "Payment Timeout #3",
                    "Paystack verification timeout after 60 seconds");
                System.out.println("    ✓ Exception #3 notification sent");
            } catch (Exception e) {
                System.out.println("    ✗ Failed to send exception #3: " + e.getMessage());
            }
        });
        
        // Start all threads
        for (Thread t : threads) {
            t.start();
        }
        
        // Wait for all threads to complete
        try {
            for (Thread t : threads) {
                t.join();
            }
            System.out.println("  ✓ All concurrent notifications sent successfully!");
        } catch (InterruptedException e) {
            System.out.println("  ✗ Thread interrupted: " + e.getMessage());
        }
    }
}

package com.space.space_bundle.config;

import com.space.space_bundle.entity.SmsPackage;
import com.space.space_bundle.repository.SmsPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsPackageDataSeeder implements CommandLineRunner {

    private final SmsPackageRepository smsPackageRepository;

    @Override
    public void run(String... args) {
        List<SmsPackage> defaultPackages = List.of(
                createPkg("Micro", 20, new BigDecimal("1.00")),
                createPkg("Starter", 430, new BigDecimal("10.00")),
                createPkg("Basic", 886, new BigDecimal("20.00")),
                createPkg("Standard", 2215, new BigDecimal("50.00")),
                createPkg("Business", 4429, new BigDecimal("100.00")),
                createPkg("Growth", 8858, new BigDecimal("200.00")),
                createPkg("Pro", 22575, new BigDecimal("500.00")),
                createPkg("Enterprise", 45580, new BigDecimal("1000.00")),
                createPkg("Business Plus", 92880, new BigDecimal("2000.00")),
                createPkg("Scale", 234350, new BigDecimal("5000.00")),
                createPkg("Scale Plus", 475150, new BigDecimal("10000.00")),
                createPkg("Corporate", 906870, new BigDecimal("19000.00")),
                createPkg("Corporate Plus", 1000000, new BigDecimal("20000.00")),
                createPkg("Enterprise Plus", 2397250, new BigDecimal("50000.00")),
                createPkg("Enterprise Max", 4478880, new BigDecimal("93000.00"))
        );

        List<SmsPackage> missingPackages = defaultPackages.stream()
                .filter(pkg -> !smsPackageRepository.existsByMessagesCount(pkg.getMessagesCount()))
                .toList();

        if (!missingPackages.isEmpty()) {
            smsPackageRepository.saveAll(missingPackages);
            log.info("Added {} missing default SMS package tiers; existing package settings were preserved.",
                    missingPackages.size());
        } else {
            log.info("All default SMS package tiers are already present.");
        }
    }

    private SmsPackage createPkg(String name, int messagesCount, BigDecimal price) {
        return SmsPackage.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .messagesCount(messagesCount)
                .price(price)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

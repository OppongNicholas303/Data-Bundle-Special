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
        if (smsPackageRepository.count() == 0) {
            log.info("No SMS packages found. Initializing default SMS package tiers...");

            List<SmsPackage> defaultPackages = List.of(
                    createPkg("Micro", 20, new BigDecimal("1.00")),
                    createPkg("Starter", 430, new BigDecimal("10.00")),
                    createPkg("Basic", 886, new BigDecimal("20.00")),
                    createPkg("Standard", 2215, new BigDecimal("50.00")),
                    createPkg("Business", 4429, new BigDecimal("100.00")),
                    createPkg("Growth", 8858, new BigDecimal("200.00")),
                    createPkg("Pro", 22575, new BigDecimal("500.00")),
                    createPkg("Enterprise", 45580, new BigDecimal("1000.00"))
            );

            smsPackageRepository.saveAll(defaultPackages);
            log.info("Successfully seeded {} default SMS packages.", defaultPackages.size());
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

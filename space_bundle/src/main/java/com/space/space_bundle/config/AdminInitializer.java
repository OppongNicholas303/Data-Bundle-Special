package com.space.space_bundle.config;

import com.space.space_bundle.entity.User;
import com.space.space_bundle.repository.UserRepository;
import com.space.space_bundle.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;

    private static final String ADMIN_EMAIL    = "samo@gmail.com";
    private static final String ADMIN_USERNAME = "samo";
    private static final String ADMIN_PASSWORD = "samo123!";
    private static final String ADMIN_PHONE    = "0000000000";

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("Admin account already exists — skipping seed.");
            return;
        }

        User admin = userRepository.save(User.builder()
                .id(UUID.randomUUID().toString())
                .username(ADMIN_USERNAME)
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .phoneNumber(ADMIN_PHONE)
                .roles(Set.of(
                        User.Role.ROLE_ADMIN.name(),
                        User.Role.ROLE_USER.name()
                ))
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .passwordLastChanged(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        walletService.createWallet(admin.getId());
        log.info("Admin account created — email: {}", ADMIN_EMAIL);
    }
}

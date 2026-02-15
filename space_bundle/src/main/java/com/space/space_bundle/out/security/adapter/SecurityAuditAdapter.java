package com.space.space_bundle.out.security.adapter;

import com.space.space_bundle.core.port.out.authenticationPort.SecurityAuditPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing SecurityAuditPort for logging security events
 */
@Slf4j
@Component
public class SecurityAuditAdapter implements SecurityAuditPort {

    @Override
    public void logSuccessfulLogin(String username, String ipAddress, String deviceInfo) {
        log.info("SECURITY_AUDIT: Successful login - Username: {}, IP: {}, Device: {}",
                username, ipAddress, deviceInfo);
    }

    @Override
    public void logFailedLogin(String username, String ipAddress, String reason) {
        log.warn("SECURITY_AUDIT: Failed login - Username: {}, IP: {}, Reason: {}",
                username, ipAddress, reason);
    }

    @Override
    public void logTokenRefresh(String username, String ipAddress) {
        log.info("SECURITY_AUDIT: Token refresh - Username: {}, IP: {}",
                username, ipAddress);
    }

    @Override
    public void logLogout(String username, String ipAddress) {
        log.info("SECURITY_AUDIT: Logout - Username: {}, IP: {}",
                username, ipAddress);
    }

    @Override
    public void logAccountLocked(String username, String reason) {
        log.warn("SECURITY_AUDIT: Account locked - Username: {}, Reason: {}",
                username, reason);
    }

    @Override
    public void logPasswordChanged(String username) {
        log.info("SECURITY_AUDIT: Password changed - Username: {}", username);
    }
}
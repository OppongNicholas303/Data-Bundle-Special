package com.space.space_bundle.core.port.out.authenticationPort;

/**
 * Output port for security audit logging
 */
public interface SecurityAuditPort {
    void logSuccessfulLogin(String email, String ipAddress, String deviceInfo);
    void logFailedLogin(String email, String ipAddress, String reason);
    void logTokenRefresh(String email, String ipAddress);
    void logLogout(String email, String ipAddress);
    void logAccountLocked(String email, String reason);
    void logPasswordChanged(String em);
}
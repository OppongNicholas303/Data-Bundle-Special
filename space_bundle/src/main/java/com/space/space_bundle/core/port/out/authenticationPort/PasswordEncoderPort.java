package com.space.space_bundle.core.port.out.authenticationPort;

/**
 * Output port for password encoding
 */
public interface PasswordEncoderPort {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}


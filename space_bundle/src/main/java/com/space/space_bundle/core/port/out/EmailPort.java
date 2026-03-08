package com.space.space_bundle.core.port.out;

public interface EmailPort {
    void sendEmail(String to, String subject, String body);
}

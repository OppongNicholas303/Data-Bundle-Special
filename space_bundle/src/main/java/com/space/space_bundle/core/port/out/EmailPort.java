package com.space.space_bundle.core.port.out;

public interface EmailPort {
    void sendEmail(String to, String subject, String body);

    void sendHtmlEmail(String to, String subject, String htmlBody);

    void sendMultipartEmail(String to, String subject, String textBody, String htmlBody);
}

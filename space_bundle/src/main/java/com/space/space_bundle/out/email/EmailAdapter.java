package com.space.space_bundle.out.email;

import com.space.space_bundle.core.port.out.EmailPort;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailAdapter implements EmailPort {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:placeholder@example.com}")
    private String fromAddress;

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("Sending simple email to: {}", to);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            log.info("Sending HTML email to: {}", to);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, "TapData Support");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send HTML email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    @Override
    public void sendMultipartEmail(String to, String subject, String textBody, String htmlBody) {
        try {
            log.info("Sending multipart email to: {}", to);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, "TapData Support");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send multipart email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send multipart email", e);
        }
    }

}
package com.space.space_bundle.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:placeholder@example.com}")
    private String from;

    public void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }

    public void sendHtml(String to, String subject, String textBody, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, "TapData Support");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Email send failed", e);
        }
    }

    public void sendPasswordReset(String to, String username, String frontendUrl, String token) {
        String link = frontendUrl + "/reset-password?token=" + token + "&email=" + to;
        String text = "Hello " + username + ",\n\nReset your password: " + link + "\n\nExpires in 1 hour.";
        String html = "<p>Hello <b>" + username + "</b>,</p><p><a href='" + link + "'>Reset Password</a></p><p>Expires in 1 hour.</p>";
        sendHtml(to, "Reset Your TapData Password", text, html);
    }
}

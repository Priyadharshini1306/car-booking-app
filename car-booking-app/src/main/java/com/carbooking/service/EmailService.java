package com.carbooking.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Send email — supports comma-separated
     * multiple recipients
     */
    public void sendEmail(
            String to,
            String subject,
            String body) {
        String[] recipients = to.split(",");
        for (String recipient : recipients) {
            String email = recipient.trim();
            if (email.isEmpty()) continue;
            try {
                SimpleMailMessage msg =
                        new SimpleMailMessage();
                msg.setTo(email);
                msg.setSubject(subject);
                msg.setText(body);
                mailSender.send(msg);
                log.info("Email sent to: {}", email);
            } catch (Exception e) {
                log.error(
                        "Failed to send email to {}: {}",
                        email, e.getMessage());
            }
        }
    }
}
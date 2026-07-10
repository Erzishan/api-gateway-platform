package com.apigateway.tenant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.enabled:false}")
    private boolean emailEnabled;

    // @Async means this runs in a separate thread
    // The caller never waits for this to complete
    @Async
    public void sendWelcomeEmail(String toEmail,
                                 String organizationName) {
        if (!emailEnabled) {
            log.info("Email disabled. Would have sent welcome " +
                    "email to: {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Welcome to API Gateway — " +
                    organizationName);
            message.setText(
                    "Hi,\n\n" +
                            "Welcome to API Gateway! Your organization " +
                            "'" + organizationName + "' " +
                            "has been successfully created.\n\n" +
                            "You can now:\n" +
                            "• Create API keys\n" +
                            "• Register your upstream routes\n" +
                            "• Monitor your API usage\n\n" +
                            "Your free plan includes 1,000,000 " +
                            "requests per month.\n\n" +
                            "Best regards,\n" +
                            "The API Gateway Team"
            );

            mailSender.send(message);
            log.info("Welcome email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}",
                    toEmail, e.getMessage());
        }
    }

    @Async
    public void sendQuotaWarningEmail(String toEmail,
                                      String organizationName,
                                      long percentUsed) {
        if (!emailEnabled) {
            log.info("Email disabled. Would have sent quota " +
                    "warning to: {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("API Gateway — Quota Warning");
            message.setText(
                    "Hi,\n\n" +
                            "Your organization '" + organizationName +
                            "' has used " + percentUsed +
                            "% of your monthly API quota.\n\n" +
                            "Consider upgrading your plan to avoid " +
                            "service interruption.\n\n" +
                            "Best regards,\n" +
                            "The API Gateway Team"
            );

            mailSender.send(message);
            log.info("Quota warning sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send quota warning to {}: {}",
                    toEmail, e.getMessage());
        }
    }
}

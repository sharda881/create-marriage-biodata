package com.biodatamaker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;

/**
 * Emails the generated bio-data PDF to the buyer. Disabled (logs + no-ops) unless
 * {@code MAIL_USERNAME} is set — lets the app run without an email provider.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean enabled;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                         @Value("${app.mail.from:}") String configuredFrom,
                         @Value("${spring.mail.username:}") String username) {
        this.mailSender = mailSender.getIfAvailable();
        this.from = StringUtils.hasText(configuredFrom) ? configuredFrom : username;
        this.enabled = this.mailSender != null && StringUtils.hasText(username);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Sends the PDF as an attachment. Throws on failure so the caller can retry/log. */
    public void sendBioDataPdf(String to, String bioDataName, byte[] pdf) {
        if (!enabled) {
            log.warn("Email delivery requested for {} but MAIL_USERNAME is not configured — skipping", to);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Your marriage bio-data PDF" + (bioDataName != null ? " — " + bioDataName : ""));
            helper.setText(
                    "Hi,\n\nYour marriage bio-data PDF is attached — thanks for your purchase!\n\n"
                            + "— Marriage Bio-Data Maker");
            String fileName = (bioDataName != null ? bioDataName : "biodata").replaceAll("\\s+", "_") + ".pdf";
            helper.addAttachment(fileName, new ByteArrayResource(pdf), "application/pdf");
            mailSender.send(message);
            log.info("Emailed bio-data PDF to {}", to);
        } catch (Exception e) {
            log.error("Failed to email bio-data PDF to {}: {}", to, e.getMessage());
            throw new RuntimeException("Could not send bio-data PDF email", e);
        }
    }
}

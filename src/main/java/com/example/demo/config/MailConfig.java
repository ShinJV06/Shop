package com.example.demo.config;

import jakarta.mail.Address;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                         MAIL CONFIGURATION                                  ║
 * ║                    Spring Boot Email Configuration                         ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  Supports: SMTP, SSL/TLS, STARTTLS, Mock Mode for Testing                  ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * @author Demo Team
 * @version 1.0.0
 * @since 2024
 */
@Configuration
public class MailConfig {

    private static final Logger logger = LoggerFactory.getLogger(MailConfig.class);

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════════

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String smtpHost;

    @Value("${spring.mail.port:587}")
    private int smtpPort;

    @Value("${spring.mail.username:huuquangttt5@gmail.com}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${app.email.mock-mode:true}")
    private boolean mockMode;

    @Value("${app.email.debug:false}")
    private boolean debugMode;

    @Value("${app.email.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${app.email.timeout:5000}")
    private int timeout;

    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN MAIL SENDER BEAN
    // ═══════════════════════════════════════════════════════════════════════════

    @Bean
    public JavaMailSender mailSender() {
        if (mockMode) {
            logger.info("📧 Mail Mode: MOCK (No emails will be sent)");
            return createMockMailSender();
        }

        logger.info("📧 Mail Mode: LIVE - Connecting to {}:{}", smtpHost, smtpPort);
        return createRealMailSender();
    }

    /**
     * Creates a real JavaMailSender with full SMTP configuration
     */
    private JavaMailSender createRealMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        // Basic Configuration
        sender.setHost(smtpHost);
        sender.setPort(smtpPort);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        // Timeout Configuration
        sender.setJavaMailProperties(buildMailProperties());

        return sender;
    }

    /**
     * Builds JavaMail properties with secure SMTP settings
     */
    private Properties buildMailProperties() {
        Properties props = new Properties();

        // ─────────────────────────────────────────────────────────────────────
        // PROTOCOL & AUTHENTICATION
        // ─────────────────────────────────────────────────────────────────────
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.auth.mechanisms", "PLAIN LOGIN");

        // ─────────────────────────────────────────────────────────────────────
        // SECURITY - STARTTLS (Port 587)
        // ─────────────────────────────────────────────────────────────────────
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        // ─────────────────────────────────────────────────────────────────────
        // TIMEOUT SETTINGS
        // ─────────────────────────────────────────────────────────────────────
        props.put("mail.smtp.connectiontimeout", connectionTimeout);
        props.put("mail.smtp.timeout", timeout);
        props.put("mail.smtp.writetimeout", timeout);

        // ─────────────────────────────────────────────────────────────────────
        // ADVANCED SETTINGS
        // ─────────────────────────────────────────────────────────────────────
        props.put("mail.smtp.quitwait", "false");
        props.put("mail.smtp.ssl.checkserveridentity", "false");
        props.put("mail.smtp.auth.login.disable", "false");
        props.put("mail.smtp.auth.plain.disable", "false");

        // Debug mode (only if enabled)
        if (debugMode) {
            props.put("mail.debug", "true");
            props.put("mail.debug.auth", "true");
        }

        return props;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MOCK MAIL SENDER - For Development & Testing
    // ═══════════════════════════════════════════════════════════════════════════

    private MockMailSender createMockMailSender() {
        return new MockMailSender(debugMode);
    }

    /**
     * Mock implementation of JavaMailSender for development/testing
     * Logs all email activity without actually sending anything
     */
    public static class MockMailSender implements JavaMailSender {

        private final boolean debugEnabled;
        private final Logger log = LoggerFactory.getLogger(MockMailSender.class);

        public MockMailSender(boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
        }

        @Override
        public MimeMessage createMimeMessage() {
            return new MockMimeMessage(debugEnabled);
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) {
            return new MockMimeMessage(debugEnabled);
        }

        @Override
        public void send(MimeMessage mimeMessage) {
            printEmailBanner("MIME EMAIL");
            try {
                MockMimeMessage mock = (MockMimeMessage) mimeMessage;
                logEmail(mock);
            } catch (Exception e) {
                log.error("❌ Error reading mock message: {}", e.getMessage());
            }
            printFooter();
        }

        @Override
        public void send(MimeMessage... mimeMessages) {
            for (MimeMessage msg : mimeMessages) {
                send(msg);
            }
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) {
            for (SimpleMailMessage msg : simpleMessages) {
                send(msg);
            }
        }

        public void send(SimpleMailMessage simpleMessage) {
            if (simpleMessage == null) return;

            printEmailBanner("SIMPLE EMAIL");
            printField("📬 TO", Arrays.toString(simpleMessage.getTo()));
            printField("📌 FROM", simpleMessage.getFrom() != null ?
                    simpleMessage.getFrom() : "N/A");
            printField("📝 SUBJECT", simpleMessage.getSubject());
            printField("💬 TEXT", simpleMessage.getText());
            printFooter();
        }

        private void logEmail(MockMimeMessage mock) {
            printField("📬 TO", mock.getRecipientsDisplay());
            printField("📌 FROM", mock.getFromDisplay());
            printField("📝 SUBJECT", mock.getSubject());
            printField("📄 CONTENT", mock.getText());
            printField("🌐 HTML", mock.isHtml() ? "Yes ✓" : "No ✗");
        }

        private void printField(String label, String value) {
            System.out.printf("   %-12s %s%n", label, value);
        }

        private void printEmailBanner(String type) {
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.printf("║  📧 %-18s Mock Mode                      ║%n", type);
            System.out.println("╠══════════════════════════════════════════════════════════╣");
        }

        private void printFooter() {
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println();
        }
    }

    /**
     * Mock MimeMessage with enhanced logging capabilities
     */
    public static class MockMimeMessage extends MimeMessage {

        private String subject;
        private String text;
        private String from;
        private String recipients;
        private boolean html;

        public MockMimeMessage(boolean debugEnabled) {
            super((jakarta.mail.Session) null);
        }

        public boolean isHtml() {
            return html;
        }

        public String getFromDisplay() {
            return from;
        }

        public String getRecipientsDisplay() {
            return recipients;
        }

        public String getSubject() {
            return subject;
        }

        public String getText() {
            return text;
        }

        @Override
        public void setSubject(String subject) {
            this.subject = subject;
        }

        @Override
        public void setText(String text) {
            this.text = text;
        }

        @Override
        public void setFrom(Address address) {
            this.from = address != null ? address.toString() : "N/A";
        }

        public void setRecipients(RecipientType type, Address[] addresses) {
            if (addresses != null && addresses.length > 0) {
                this.recipients = Arrays.stream(addresses)
                        .map(Object::toString)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("N/A");
            } else {
                this.recipients = "N/A";
            }
        }
    }
}

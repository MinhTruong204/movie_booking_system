package com.viecinema.auth.service;

import com.viecinema.auth.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Gửi email xác thực bất đồng bộ để không block request đăng ký.
     */
    @Async
    public void sendVerificationEmail(User user, String token) {
        try {
            String verifyUrl = baseUrl + "/api/auth/verify-email?token=" + token;

            Context context = new Context();
            context.setVariable("fullName", user.getFullName());
            context.setVariable("verifyUrl", verifyUrl);
            context.setVariable("expiryHours", 24);

            String htmlContent = templateEngine.process("email/verification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "VieCinema");
            helper.setTo(user.getEmail());
            helper.setSubject("🎬 VieCinema - Xác thực địa chỉ email của bạn");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", user.getEmail());

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
            // Không throw exception để không làm fail transaction đăng ký
        }
    }
}

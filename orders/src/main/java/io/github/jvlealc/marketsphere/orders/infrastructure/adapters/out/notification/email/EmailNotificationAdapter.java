package io.github.jvlealc.marketsphere.orders.infrastructure.notification;

import io.github.jvlealc.marketsphere.orders.application.ports.out.notification.NotificationPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.notification.OrderPaidNotification;
import io.github.jvlealc.marketsphere.orders.infrastructure.exception.NotificationException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Component
public class EmailNotificationAdapter implements NotificationPort {

    private static final String ENCODING = "UTF-8";

    private final JavaMailSender mailSender;
    private final String from;
    private final Resource logo;

    public EmailNotificationAdapter(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String from,
            @Value("classpath:static/ms-logo.png") Resource logo
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.logo = logo;
    }

    @Override
    public void sendPaidOrderConfirmation(final OrderPaidNotification notification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, ENCODING);

            helper.setFrom(from);
            helper.setTo(notification.customer().email());
            helper.setSubject("Market Sphere - Pagamento Confirmado");

            String htmlBody = generateHtmlBody(notification.customer().fullName(), notification.orderId(), notification.orderTotal());

            helper.setText(htmlBody, true);
            helper.addInline("msLogo", logo);

            mailSender.send(message);

        } catch (MessagingException | MailException e) {
            throw new NotificationException("Error sending paid order confirmation email. Order ID: " + notification.orderId(), e);
        }
    }

    private static String generateHtmlBody(final String customerFullName, final Long orderId, final BigDecimal orderTotal) {
        return """
                <html>
                    <body>
                        <h1>
                            Market Sphere
                        </h1>
                        <h2>Olá, %s!</h2>
                        <p>Boa notícia! O pagamento do seu pedido <strong>#%d</strong> foi confirmado.</p>
                        <p><strong>Total: %s</strong></p>
                        <br>
                        <p>Obrigado por comprar conosco!</p>
                    </body>
                </html>
                """.formatted(customerFullName, orderId, formatCurrencyAsBRL(orderTotal));
    }

    private static String formatCurrencyAsBRL(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Order total must not be null");
        }

        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Order total must not be negative");
        }

        return NumberFormat
                .getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
                .format(value);
    }
}

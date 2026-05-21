package io.github.jvlealc.marketsphere.shipping.notification;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class ShipmentEmailSender {

    private static final Logger log = LoggerFactory.getLogger(ShipmentEmailSender.class);

    private static final String ENCODING = "UTF-8";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    private final JavaMailSender mailSender;
    private final String from;
    private final Resource logo;

    public ShipmentEmailSender(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String from,
            @Value("classpath:static/ms-logo.png") Resource logo
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.logo = logo;
    }

    @Async
    public void sendShipmentConfirmation(ShipmentConfirmationNotification notification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, ENCODING);

            helper.setFrom(from);
            helper.setTo(notification.customerEmail());
            helper.setSubject("Market Sphere - Pedido Enviado");

            String htmlBody = this.generateHtmlBody(
                    notification.customerName(),
                    notification.orderId(),
                    notification.trackingCode().toString(),
                    notification.shippedAt()
            );

            helper.setText(htmlBody, true);
            helper.addInline("msLogo", logo);

            mailSender.send(message);

            log.info("Shipment email sent successfully for order ID: {}", notification.orderId());
        } catch (Exception ignored) {
        }
    }

    private String generateHtmlBody(
            String customerName,
            Long orderId,
            String trackingCode,
            Instant shippedAt
    ) {
        return """
                <html>
                    <body>
                        <h1>Market Sphere</h1>
                        <h2>Olá, %s!</h2>
                        <p>Boa notícia! O seu pedido <strong>#%d</strong> foi enviado.</p>
                        <p><strong>Código de rastreio:</strong> %s</p>
                        <p><strong>Data de envio:</strong> %s</p>
                        <br>
                        <p>Obrigado por comprar conosco!</p>
                    </body>
                </html>
                """.formatted(
                customerName,
                orderId,
                trackingCode,
                DATE_TIME_FORMATTER.format(shippedAt)
        );
    }

}

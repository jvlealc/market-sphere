package io.github.jvlealc.marketsphere.orders.service.notification;

import io.github.jvlealc.marketsphere.orders.client.customers.representation.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.model.Order;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("classpath:static/ms-logo.png")
    private Resource logo;

    @Async
    @Override
    public void sendShipmentConfirmationEmail(CustomerRepresentation customer, Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(customer.email());
            helper.setSubject("Seu pedido #" + order.getId() + " foi enviado!");

            String htmlBody = this.generateHtmlBody(
                    customer.fullName(),
                    order.getId(),
                    order.getTrackingCode().toString()
            );

            helper.setText(htmlBody, true);
            helper.addInline("msLogo", logo);

            mailSender.send(message);
            log.info("Shipment email sent successfully for order ID: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send shipment email for order ID: {}. Error: {}",
                    order.getId(), e.getMessage());
        }
    }

    private String generateHtmlBody(
            final String customerFullName,
            final Long orderId,
            final String trackingCode
    ) {
        return String.format(
                // <img src="cid:msLogo" alt="enterprise logo" style="width:100px; height:150px;" />
                """
                <html>
                    <body>
                        <h1>
                            Market Sphere
                        </h1>
                        <h2>Olá, %s!</h2>
                        <p>Boas notícias! Seu pedido <strong>#%d</strong> foi enviado.</p>
                        <p>O código de rastreio é: <strong>%s</strong></p>
                        <p>Obrigado por comprar conosco!</p>
                    </body>
                </html>
                """,
                customerFullName,
                orderId,
                trackingCode
        );
    }
}

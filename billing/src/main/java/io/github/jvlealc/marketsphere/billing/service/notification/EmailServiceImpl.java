package io.github.jvlealc.marketsphere.billing.service.notification;

import io.github.jvlealc.marketsphere.billing.bucket.BucketFile;
import io.github.jvlealc.marketsphere.billing.bucket.BucketService;
import io.github.jvlealc.marketsphere.billing.model.Order;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final BucketService bucketService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender, BucketService bucketService) {
        this.mailSender = mailSender;
        this.bucketService = bucketService;
    }

    @Async
    @Override
    public void sendInvoiceEmailWithAttachment(Order order, String fileName, String invoiceUrl) {
        try {
            // Buscar arquivo no bucket
            BucketFile invoiceFile = bucketService.download(fileName);

            // Preparar email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8"); // multipart true -> necessário para anexos

            helper.setFrom(this.fromEmail);
            helper.setTo(order.customer().email());
            helper.setSubject("Market Sphere - Fatura do Pedido #" + order.orderId());

            // Anexar o arquivo
            // Ler o Stream para bytes
            byte[] invoiceBytes;
            try (InputStream is = invoiceFile.inputStream()) {
                invoiceBytes = is.readAllBytes();
            }
            // Anexa os bytes ao email
            helper.addAttachment(fileName, new ByteArrayResource(invoiceBytes), invoiceFile.mediaType().toString());

            // Construir corpo do email
            String htmlBody = this.generateHtmlBody(
                    order.customer().fullName(),
                    order.orderId(),
                    invoiceUrl
            );
            helper.setText(htmlBody, true);

            // Enviar email
            mailSender.send(message);
            log.info("Invoice email sent successfully for order ID: {}", order.orderId());
        } catch (Exception e) {
            log.error("Failed to send invoice email for order ID: {}. Error: {}",
                    order.orderId(), e.getMessage(), e);
        }
    }

    private String generateHtmlBody(final String customerName, final Long orderId, final String invoiceUrl) {
        return String.format(
                """
                <html>
                    <body>
                        <h1>Olá, %s!</h1>
                        <p>A fatura do seu pedido <strong>#%d</strong> está em anexo.</p>
                        <p>Se preferir, você também pode baixá-la clicando no link abaixo:</p>
                        <p><a href="%s" target="_blank" rel="noopener noreferrer">Baixar Fatura</a></p>
                        <p>Obrigado!</p>
                    </body>
                </html>
                """,
                customerName,
                orderId,
                invoiceUrl
        );
    }
}

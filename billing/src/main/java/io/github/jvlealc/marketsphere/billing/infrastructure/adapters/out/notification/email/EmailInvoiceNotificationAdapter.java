package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.out.notification.email;

import io.github.jvlealc.marketsphere.billing.application.exception.OutboxDeliveryException;
import io.github.jvlealc.marketsphere.billing.application.model.document.RetrievedInvoiceDocument;
import io.github.jvlealc.marketsphere.billing.application.model.notification.InvoiceNotification;
import io.github.jvlealc.marketsphere.billing.application.ports.out.InvoiceNotificationPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Entrega a notificação de nota emitida por e-mail, com o PDF anexo.
 * <p>
 * Toda falha vira {@link OutboxDeliveryException} — retentável. Um SMTP que recusa conexão hoje pode
 * aceitar daqui a um minuto, e é o worker de relay quem decide quando insistir e quando desistir. O
 * adaptador não tem opinião sobre isso; ele só traduz o modo de falha da tecnologia para o vocabulário da
 * porta.
 */
@Component
class EmailInvoiceNotificationAdapter implements InvoiceNotificationPort {

    private static final String SUBJECT_TEMPLATE = "Market Sphere - Fatura do Pedido #%d";
    private static final String ATTACHMENT_TEMPLATE = "NF-%s.pdf";

    private final JavaMailSender mailSender;
    private final String from;

    EmailInvoiceNotificationAdapter(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String from
    ) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void notifyInvoiceIssued(InvoiceNotification notification, RetrievedInvoiceDocument document) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(from);
            helper.setTo(notification.recipientEmail());
            helper.setSubject(SUBJECT_TEMPLATE.formatted(notification.orderId()));
            helper.setText(htmlBody(notification), true);
            helper.addAttachment(
                    ATTACHMENT_TEMPLATE.formatted(notification.invoiceId()),
                    new ByteArrayResource(document.content()),
                    document.contentType()
            );

            mailSender.send(message);

        } catch (MessagingException | MailException deliveryFailure) {
            throw new OutboxDeliveryException(
                    "Failed to send the invoice e-mail for order %d to %s"
                            .formatted(notification.orderId(), notification.recipientEmail()),
                    deliveryFailure
            );
        }
    }

    /**
     * O nome do cliente vem de cadastro e entra em HTML — precisa ser escapado. É injeção de HTML em corpo
     * de e-mail: impacto baixo, custo de evitar igual a cinco linhas.
     */
    private static String htmlBody(InvoiceNotification notification) {
        return """
                <html>
                    <body>
                        <h1>Olá, %s!</h1>
                        <p>A fatura do seu pedido <strong>#%d</strong> segue em anexo.</p>
                        <br>
                        <p>Obrigado por comprar conosco!</p>
                    </body>
                </html>
                """
                .formatted(escapeHtml(notification.recipientName()), notification.orderId());
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

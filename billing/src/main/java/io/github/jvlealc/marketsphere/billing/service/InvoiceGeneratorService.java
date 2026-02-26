package io.github.jvlealc.marketsphere.billing.service;

import io.github.jvlealc.marketsphere.billing.bucket.BucketFile;
import io.github.jvlealc.marketsphere.billing.bucket.BucketService;
import io.github.jvlealc.marketsphere.billing.bucket.exception.StorageAccessException;
import io.github.jvlealc.marketsphere.billing.exception.InvoiceGenerationException;
import io.github.jvlealc.marketsphere.billing.exception.OrderProcessingException;
import io.github.jvlealc.marketsphere.billing.model.Order;
import io.github.jvlealc.marketsphere.billing.publisher.OrderBillingPublisher;
import io.github.jvlealc.marketsphere.billing.service.notification.EmailService;
import io.github.jvlealc.marketsphere.billing.subscriber.event.OrderPaidEvent;
import io.github.jvlealc.marketsphere.billing.subscriber.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
@Slf4j
public class InvoiceGeneratorService {

    private final InvoiceService invoiceService;
    private final BucketService bucketService;
    private final OrderMapper orderMapper;
    private final OrderBillingPublisher orderBillingPublisher;
    private final EmailService emailService;

    public InvoiceGeneratorService(
            BucketService bucketService,
            InvoiceService invoiceService,
            OrderMapper orderMapper,
            OrderBillingPublisher orderBillingPublisher,
            EmailService emailService
    ) {
        this.bucketService = bucketService;
        this.invoiceService = invoiceService;
        this.orderMapper = orderMapper;
        this.orderBillingPublisher = orderBillingPublisher;
        this.emailService = emailService;
    }

    public void generate(OrderPaidEvent orderPaidEvent) {
        log.info("Generating invoice for order with ID {}.", orderPaidEvent.orderId());
        try {
            // Converter para o Order do domínio
            Order order = orderMapper.toDomainModel(orderPaidEvent);

            // Gerar o array de bytes
            byte[] bytes = invoiceService.generateFromOrder(order);

            // Construir o bucketFile
            String fileName = String.format("invoice_order_%d.pdf", order.orderId());
            BucketFile bucketFile = new BucketFile(
                    fileName,
                    new ByteArrayInputStream(bytes),
                    MediaType.APPLICATION_PDF,
                    bytes.length
            );

            // Realizar upload para cloud
            bucketService.upload(bucketFile);
            log.info("Generated invoice, file name: {}.", bucketFile.name());

            // extrair a URL da fatura
            String invoiceUrl = bucketService.generatePresignedUrl(bucketFile.name());

            // AÇÃO NÃO-CRÍTICA
            //Enviar email @Async
            emailService.sendInvoiceEmailWithAttachment(order, fileName, invoiceUrl);

            // AÇÃO CRÍTICA
            // delegar a publicação do evento no Kafka ao Publish de faturamento
            orderBillingPublisher.publish(order, invoiceUrl);


        } catch (InvoiceGenerationException | StorageAccessException e) {
            log.error("Error in the invoice generation process for order ID: {}. Error: {}",
                    orderPaidEvent.orderId(), e.getMessage(), e);
            throw new OrderProcessingException("Failed to process invoice for order ID: " + orderPaidEvent.orderId(), e);
        }
    }
}

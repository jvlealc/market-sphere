package io.github.jvlealc.marketsphere.billing.service;

import io.github.jvlealc.marketsphere.billing.bucket.BucketFile;
import io.github.jvlealc.marketsphere.billing.bucket.BucketService;
import io.github.jvlealc.marketsphere.billing.bucket.exception.StorageAccessException;
import io.github.jvlealc.marketsphere.billing.exception.InvoiceGenerationException;
import io.github.jvlealc.marketsphere.billing.exception.OrderProcessingException;
import io.github.jvlealc.marketsphere.billing.model.Order;
import io.github.jvlealc.marketsphere.billing.publisher.OrderBilledPublisher;
import io.github.jvlealc.marketsphere.billing.publisher.event.OrderBilledCustomerPayload;
import io.github.jvlealc.marketsphere.billing.publisher.event.OrderBilledEvent;
import io.github.jvlealc.marketsphere.billing.service.notification.EmailService;
import io.github.jvlealc.marketsphere.billing.subscriber.event.OrderPaidEvent;
import io.github.jvlealc.marketsphere.billing.subscriber.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.Instant;

@Component
@Slf4j
public class InvoiceGeneratorService {

    private final InvoiceService invoiceService;
    private final BucketService bucketService;
    private final OrderMapper orderMapper;
    private final OrderBilledPublisher orderBilledPublisher;
    private final EmailService emailService;

    public InvoiceGeneratorService(
            BucketService bucketService,
            InvoiceService invoiceService,
            OrderMapper orderMapper,
            OrderBilledPublisher orderBilledPublisher,
            EmailService emailService
    ) {
        this.bucketService = bucketService;
        this.invoiceService = invoiceService;
        this.orderMapper = orderMapper;
        this.orderBilledPublisher = orderBilledPublisher;
        this.emailService = emailService;
    }

    public void generate(OrderPaidEvent orderPaidEvent) {
        log.info("Generating invoice for order with ID {}.", orderPaidEvent.orderId());

        Order order;
        String fileName;
        String invoiceUrl;
        try {
            // Converter para o Order do domínio
            order = orderMapper.toDomainModel(orderPaidEvent);

            // Gerar o array de bytes
            byte[] bytes = invoiceService.generateFromOrder(order);

            // Construir o bucketFile
            fileName = String.format("invoice_order_%d.pdf", order.orderId());
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
            invoiceUrl = bucketService.generatePresignedUrl(bucketFile.name());

            // AÇÃO CRÍTICA
            Instant billedAt = Instant.now();
            OrderBilledEvent event = new OrderBilledEvent(
                    order.orderId(),
                    invoiceUrl,
                    billedAt,
                    new OrderBilledCustomerPayload(
                            order.customer().id(),
                            order.customer().fullName(),
                            order.customer().email()
                    )
            );

            orderBilledPublisher.publish(event);

        } catch (InvoiceGenerationException | StorageAccessException e) {
            log.error("Error in the invoice generation process for order ID: {}. Error: {}",
                    orderPaidEvent.orderId(), e.getMessage(), e);
            throw new OrderProcessingException("Failed to process invoice for order ID: " + orderPaidEvent.orderId(), e);
        }

        try {
            // AÇÃO NÃO-CRÍTICA
            emailService.sendInvoiceEmailWithAttachment(order, fileName, invoiceUrl);
        } catch (Exception e) {
            log.error("Failed to send invoice email for order ID: {}. Error: {}", order.orderId(), e.getMessage(), e);
        }
    }
}

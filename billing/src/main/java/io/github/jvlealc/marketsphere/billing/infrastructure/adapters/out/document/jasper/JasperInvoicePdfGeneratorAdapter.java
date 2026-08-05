package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.out.document.jasper;

import io.github.jvlealc.marketsphere.billing.application.model.document.GeneratedInvoiceDocument;
import io.github.jvlealc.marketsphere.billing.application.model.order.OrderPaidSnapshot;
import io.github.jvlealc.marketsphere.billing.application.ports.out.InvoiceDocumentGeneratorPort;
import io.github.jvlealc.marketsphere.billing.infrastructure.exception.InvoiceDocumentGenerationException;
import io.github.jvlealc.marketsphere.billing.infrastructure.i18n.MessageTranslator;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

@Component
class JasperInvoicePdfGeneratorAdapter implements InvoiceDocumentGeneratorPort {

    private static final Locale INVOICE_LOCALE = Locale.forLanguageTag("pt-BR");
    private static final ZoneId INVOICE_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm:ss",
                    INVOICE_LOCALE
            )
            .withZone(INVOICE_ZONE);

    private static final String ORDER_BILLED_OBSERVATION_MESSAGE_KEY = "order.billed.awaiting.shipment";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String PDF_FILE_EXTENSION = "pdf";

    private final JasperReport invoiceReport;
    private final byte[] logoContent;
    private final MessageTranslator messageTranslator;

    JasperInvoicePdfGeneratorAdapter(
            @Value("classpath:reports/invoice.jrxml") Resource invoiceTemplateResource,
            @Value("classpath:reports/ms-logo.png") Resource logoResource,
            MessageTranslator messageTranslator
    ) {
        this.messageTranslator = messageTranslator;

        try (
                InputStream templateInputStream = invoiceTemplateResource.getInputStream();
                InputStream logoInputStream = logoResource.getInputStream()
        ){
            this.invoiceReport = JasperCompileManager.compileReport(templateInputStream);
            this.logoContent = logoInputStream.readAllBytes();

        } catch (IOException | JRException e) {
            throw new IllegalStateException("Failed to initialize Jasper invoice template", e);
        }
    }

    @Override
    public GeneratedInvoiceDocument generate(OrderPaidSnapshot order) {
        try {
            Map<String, Object> params = createReportParams(order);

            // Adicionando a coleção de itens do pedido ao campo Detail do relatório
            JRBeanCollectionDataSource collectionDataSource = new JRBeanCollectionDataSource(
                    order.items()
                            .stream()
                            .map(JasperInvoiceItemRow::from)
                            .toList()
            );

            // Preencher o relatório
            JasperPrint jasperPrint = JasperFillManager.fillReport(invoiceReport, params, collectionDataSource);

            // Transformar o jasperPrint em um array de bytes em formato PDF
            byte[] pdfContent = JasperExportManager.exportReportToPdf(jasperPrint);

            return new GeneratedInvoiceDocument(pdfContent, PDF_CONTENT_TYPE, PDF_FILE_EXTENSION);

        } catch (JRException e) {
            throw new InvoiceDocumentGenerationException("Failed to generate invoice PDF for order ID: " + order.orderId(), e);
        }
    }

    private Map<String, Object> createReportParams(OrderPaidSnapshot order) {
        String observation = messageTranslator.translate(ORDER_BILLED_OBSERVATION_MESSAGE_KEY, INVOICE_LOCALE);

        // Definindo os valores dos parâmetros que irão popular o Column Header do relatório
        Map<String, Object> params = new HashMap<>();
        params.put("NAME", order.customer().fullName());
        params.put("NATIONAL_ID", order.customer().nationalId());
        params.put("EMAIL", order.customer().email());
        params.put("PHONE_NUMBER", order.customer().phoneNumber());
        params.put("POSTAL_CODE", order.customer().address().postalCode());
        params.put("ADDRESS_NUMBER", order.customer().address().houseNumber());
        params.put("NEIGHBORHOOD", order.customer().address().neighborhood());
        params.put("STREET", order.customer().address().street());
        params.put("ADDRESS_COMPLEMENT", order.customer().address().complement());
        params.put("CITY", order.customer().address().city());
        params.put("STATE", order.customer().address().state());
        params.put("ORDER_ID", order.orderId());
        params.put("ORDER_DATE", formatDate(order.orderDate()));
        params.put("ORDER_OBSERVATIONS", observation);
        params.put("ORDER_TOTAL", order.total());
        params.put("LOGO", new ByteArrayInputStream(logoContent));
        params.put(JRParameter.REPORT_LOCALE, INVOICE_LOCALE);
        params.put(JRParameter.REPORT_TIME_ZONE, TimeZone.getTimeZone(INVOICE_ZONE));

        return params;
    }

    private String formatDate(TemporalAccessor date) {
        if (date == null) {
            throw new InvoiceDocumentGenerationException("Order date is required to generate invoice document");
        }
        return ORDER_DATE_FORMATTER.format(date);
    }
}

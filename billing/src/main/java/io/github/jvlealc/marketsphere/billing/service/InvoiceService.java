package io.github.jvlealc.marketsphere.billing.service;

import io.github.jvlealc.marketsphere.billing.exception.InvoiceGenerationException;
import io.github.jvlealc.marketsphere.billing.model.Order;
import io.github.jvlealc.marketsphere.billing.translator.MessageTranslator;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class InvoiceService {

    private static final String DATE_PATTERN_PT_BR = "dd/MM/yyyy HH:mm:ss";
    private static final String LOCALE_TAG_PT_BR = "pt_BR";
    private static final String ZONE_ID_SAO_PAULO = "America/Sao_Paulo";
    private static final String ORDER_BILLED_OBSERVATION_MESSAGE_KEY = "order.billed.awaiting.shipment";

    private final MessageTranslator messageTranslator;

    // Resource → Objeto utilizado quando é necessário injetar algum recurso da pasta resources no código.
    @Value("classpath:reports/invoice.jrxml")
    private Resource invoice;

    @Value("classpath:reports/ms-logo.png")
    private Resource logo;

    public InvoiceService(MessageTranslator messageTranslator) {
        this.messageTranslator = messageTranslator;
    }

    public byte[] generateFromOrder(Order order) {
        try (InputStream inputStream = invoice.getInputStream()) {

            String translatedObservation = messageTranslator.translate(ORDER_BILLED_OBSERVATION_MESSAGE_KEY);

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
            params.put("ORDER_DATE", this.formatDate(order.orderDate()));
            params.put("ORDER_OBSERVATIONS", translatedObservation);
            params.put("ORDER_TOTAL", order.total());

            // Passando a logo
            params.put("LOGO", logo.getFile().getAbsolutePath());

            // Adicionando a coleção de itens do pedido ao campo Detail do relatório
            JRBeanCollectionDataSource jrBeanCollectionDataSource = new JRBeanCollectionDataSource(order.orderItems());

            // Compilar o relatório
            JasperReport jasperReport = JasperCompileManager.compileReport(inputStream);

            // Preencher o relatório
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, jrBeanCollectionDataSource);

            // Transformar o jasperPrint em um array de bytes em formato PDF
            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (JRException | IOException e) {
            throw new InvoiceGenerationException("Failed to generate invoice PDF for order ID:" + order.orderId(), e);
        }
    }

    private String formatDate(TemporalAccessor date) {
        if (date == null) {
            return "Date unavailable.";
        }
        var formatter = DateTimeFormatter.ofPattern(DATE_PATTERN_PT_BR, Locale.forLanguageTag(LOCALE_TAG_PT_BR))
                .withZone(ZoneId.of(ZONE_ID_SAO_PAULO));
        return formatter.format(date);
    }
}

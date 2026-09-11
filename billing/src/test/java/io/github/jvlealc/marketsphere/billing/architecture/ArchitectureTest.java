package io.github.jvlealc.marketsphere.billing.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.jvlealc.marketsphere.billing.application.outbox.payload.OutboxPayload;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Impõe as fronteiras hexagonais deste módulo.
 */
@AnalyzeClasses(packages = ArchitectureTest.ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String ROOT_PACKAGE = "io.github.jvlealc.marketsphere.billing";

    private static final String DOMAIN = "..billing.domain..";
    private static final String APPLICATION = "..billing.application..";
    private static final String APPLICATION_IDENTITY = "..billing.application.identity..";
    private static final String INFRASTRUCTURE = "..billing.infrastructure..";

    private static final String ADAPTER_INBOUND = "..billing.infrastructure.adapter.inbound..";
    private static final String ADAPTER_INBOUND_REST = "..billing.infrastructure.adapter.inbound.rest..";
    private static final String ADAPTER_INBOUND_KAFKA = "..billing.infrastructure.adapter.inbound.kafka..";
    private static final String ADAPTER_INBOUND_SCHEDULING = "..billing.infrastructure.adapter.inbound.scheduling..";

    private static final String ADAPTER_OUTBOUND = "..billing.infrastructure.adapter.outbound..";
    private static final String ADAPTER_OUTBOUND_OUTBOX_KAFKA = "..billing.infrastructure.adapter.outbound.outbox.kafka..";
    private static final String ADAPTER_OUTBOUND_NOTIFICATION = "..billing.infrastructure.adapter.outbound.notification..";
    private static final String ADAPTER_OUTBOUND_DOCUMENT = "..billing.infrastructure.adapter.outbound.document..";
    private static final String CONFIG_MINIO = "..billing.infrastructure.config.minio..";

    // ------------------------------------------------------------------ camadas

    @ArchTest
    static final ArchRule domain_depends_on_no_other_layer = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE)
            .because("the domain is the centre of the hexagon: everything points inwards, nothing points out");

    @ArchTest
    static final ArchRule domain_is_framework_free = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.validation..",
                    "com.fasterxml.jackson..",
                    "lombok..",
                    "io.minio..",
                    "net.sf.jasperreports..",
                    "org.apache.kafka.."
            )
            .because("Invoice must stay testable with plain new(), with no container and no annotation processor");

    @ArchTest
    static final ArchRule application_does_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
            .because("adapters are chosen at wiring time; the application only knows its own ports");

    /**
     * Com exceção de {@code org.springframework.stereotype} e {@code org.springframework.transaction} por
     * decisão da atomicidade.
     */
    @ArchTest
    static final ArchRule application_is_free_of_delivery_technology = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.fasterxml.jackson..",
                    "jakarta.persistence..",
                    "jakarta.mail..",
                    "io.minio..",
                    "net.sf.jasperreports..",
                    "org.apache.kafka..",
                    "org.springframework.data..",
                    "org.springframework.http..",
                    "org.springframework.mail..",
                    "org.springframework.web.."
            )
            .because("the application layer orchestrates ports; which library speaks JSON, SMTP, SQL, PDF or HTTP is an adapter's business");

    /**
     * O UUIDv7 não é detalhe substituível: o id da linha de outbox é o header {@code event-id} publicado e
     * vira o {@code causation-id} a jusante, e a ordenação temporal dele é o que mantém local o índice
     * usado pela reivindicação.
     */
    @ArchTest
    static final ArchRule uuid_generation_is_confined_to_the_identity_package = noClasses()
            .that().resideOutsideOfPackage(APPLICATION_IDENTITY)
            .should().dependOnClassesThat().resideInAnyPackage("com.fasterxml.uuid..")
            .because("UUIDv7 is a published contract and an index-locality decision, not an incidental dependency");

    @ArchTest
    static final ArchRule outbox_payloads_are_free_of_serialization_technology = noClasses()
            .that().implement(OutboxPayload.class)
            .should().dependOnClassesThat().resideInAnyPackage("com.fasterxml.jackson..")
            .because("the stored payload is the contract published verbatim; the library that renders it as JSON belongs to the codec adapter");

    // ------------------------------------------------------------------- nomes

    @ArchTest
    static final ArchRule ports_are_interfaces_owned_by_the_application = classes()
            .that().haveSimpleNameEndingWith("Port")
            .should().beInterfaces()
            .andShould().resideInAPackage(APPLICATION)
            .because("a port is a contract owned by the side that needs the capability; anything with a body is an adapter in disguise");

    @ArchTest
    static final ArchRule use_cases_live_in_the_application_layer = classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .should().resideInAPackage(APPLICATION)
            .because("the inbound boundary should be recognisable by name alone, wherever its capability package sits");

    @ArchTest
    static final ArchRule no_field_injection = noFields()
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .orShould().beAnnotatedWith("org.springframework.beans.factory.annotation.Value")
            .because("constructor injection makes dependencies explicit and the class usable without a container");

    // ------------------------------------------------------ direção dos adapters

    /**
     * As duas direções não se conhecem. Um adapter de entrada e um de saída só se encontram passando pela
     * aplicação; o que os dois precisam compartilhar sobe para fora de {@code adapter}, como
     * {@code infrastructure.kafka} e {@code infrastructure.config.outbox}.
     */
    @ArchTest
    static final ArchRule inbound_adapters_do_not_depend_on_outbound_adapters = noClasses()
            .that().resideInAPackage(ADAPTER_INBOUND)
            .should().dependOnClassesThat().resideInAPackage(ADAPTER_OUTBOUND)
            .because("a driving adapter must not reach for a driven one: they meet through the application, never directly");

    @ArchTest
    static final ArchRule outbound_adapters_do_not_depend_on_inbound_adapters = noClasses()
            .that().resideInAPackage(ADAPTER_OUTBOUND)
            .should().dependOnClassesThat().resideInAPackage(ADAPTER_INBOUND)
            .because("the same boundary, read from the other side");

    // ---------------------------------------------------------------- posições

    @ArchTest
    static final ArchRule rest_controllers_live_in_the_inbound_rest_adapter = classes()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .or().areAnnotatedWith("org.springframework.web.bind.annotation.RestControllerAdvice")
            .should().resideInAPackage(ADAPTER_INBOUND_REST)
            .because("HTTP is a delivery mechanism, and delivery mechanisms are inbound adapters");

    @ArchTest
    static final ArchRule kafka_listeners_live_in_the_inbound_kafka_adapter = methods()
            .that().areAnnotatedWith("org.springframework.kafka.annotation.KafkaListener")
            .should().beDeclaredInClassesThat().resideInAPackage(ADAPTER_INBOUND_KAFKA)
            .because("a consumer is driven by the broker: same boundary as a controller, different transport");

    @ArchTest
    static final ArchRule scheduled_methods_live_in_the_inbound_scheduling_adapter = methods()
            .that().areAnnotatedWith("org.springframework.scheduling.annotation.Scheduled")
            .should().beDeclaredInClassesThat().resideInAPackage(ADAPTER_INBOUND_SCHEDULING)
            .because("the clock drives the application just as the broker and HTTP do");

    @ArchTest
    static final ArchRule jpa_entities_live_in_an_outbound_adapter = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAPackage(ADAPTER_OUTBOUND)
            .because("an entity is a persistence detail, not a shape the application is allowed to see");

    @ArchTest
    static final ArchRule spring_data_repositories_live_in_an_outbound_adapter = classes()
            .that().areAssignableTo("org.springframework.data.repository.Repository")
            .should().resideInAPackage(ADAPTER_OUTBOUND)
            .because("Spring Data interfaces implement no port: they are the driven side of one");

    @ArchTest
    static final ArchRule kafka_publishing_lives_in_the_outbox_kafka_adapter = noClasses()
            .that().resideOutsideOfPackages(ADAPTER_OUTBOUND_OUTBOX_KAFKA, ADAPTER_INBOUND_KAFKA)
            .should().dependOnClassesThat().resideInAPackage("org.springframework.kafka.core..")
            .because("every outbound publish in this service is an outbox publish; the inbound side holds a template only to feed the DLT");

    @ArchTest
    static final ArchRule mail_sending_lives_in_the_outbound_notification_adapter = noClasses()
            .that().resideOutsideOfPackage(ADAPTER_OUTBOUND_NOTIFICATION)
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.mail..", "jakarta.mail..")
            .because("SMTP is one way of notifying; the port that names the intention must not know which");

    /**
     * Renderizar o PDF e guardar o objeto são duas metades da mesma capability, e é por isso que os dois
     * adapters moram sob {@code adapter.outbound.document}. O {@code config.minio} entra na lista porque
     * é onde o cliente é construído, não onde ele é usado.
     */
    @ArchTest
    static final ArchRule invoice_document_technology_is_confined_to_its_adapter = noClasses()
            .that().resideOutsideOfPackages(ADAPTER_OUTBOUND_DOCUMENT, CONFIG_MINIO)
            .should().dependOnClassesThat().resideInAnyPackage("net.sf.jasperreports..", "io.minio..")
            .because("that the invoice document is a Jasper report kept in object storage is a fact three ports exist to hide");
}

package io.github.jvlealc.marketsphere.orders.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.OutboxPayload;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Impõe as fronteiras hexagonais deste módulo.
 *
 * <h4>Por que a maioria das regras ancora em nome, e não em pacote</h4>
 * Uma regra do tipo "o que mora em {@code ..application.ports..} deve ser interface" passa por
 * <em>vacuidade</em> no dia em que esse pacote deixa de existir: ela não encontra classe alguma e fica
 * verde. É assim que uma reorganização de pacotes silencia a verificação que deveria protegê-la.
 * <p>
 * Ancorar no nome ({@code *Port}, {@code *UseCase}) e na dependência proibida inverte isso: a regra
 * segue a classe para onde ela for, e o que ela impede continua impedido depois do move.
 */
@AnalyzeClasses(packages = ArchitectureTest.ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String ROOT_PACKAGE = "io.github.jvlealc.marketsphere.orders";

    private static final String DOMAIN = "..orders.domain..";
    private static final String APPLICATION = "..orders.application..";
    private static final String APPLICATION_IDENTITY = "..orders.application.identity..";
    private static final String INFRASTRUCTURE = "..orders.infrastructure..";

    private static final String CONFIG = "..orders.infrastructure.config..";

    private static final String ADAPTERS_IN_REST = "..orders.infrastructure.adapters.in.rest..";
    private static final String ADAPTERS_IN_MESSAGING = "..orders.infrastructure.adapters.in.messaging..";
    private static final String ADAPTERS_IN_SCHEDULER = "..orders.infrastructure.adapters.in.scheduler..";
    private static final String ADAPTERS_OUT_PERSISTENCE = "..orders.infrastructure.adapters.out.persistence..";
    private static final String ADAPTERS_OUT_MESSAGING = "..orders.infrastructure.adapters.out.messaging..";
    private static final String ADAPTERS_OUT_NOTIFICATION = "..orders.infrastructure.adapters.out.notification..";
    private static final String ADAPTERS_OUT_CLIENT = "..orders.infrastructure.adapters.out.client..";

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
                    "feign..",
                    "org.apache.kafka.."
            )
            .because("Order must stay testable with plain new(), with no container and no annotation processor");

    @ArchTest
    static final ArchRule application_does_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
            .because("adapters are chosen at wiring time; the application only knows its own ports");

    /**
     * Substitui a antiga lista de permitidos de {@code ..application.model..}. A proibição vale agora para
     * a camada inteira: o que impedia um payload de ganhar uma anotação Jackson passa a impedir também
     * que um caso de uso receba um {@code Pageable} ou devolva um {@code ResponseEntity}.
     * <p>
     * {@code org.springframework.stereotype} e {@code org.springframework.transaction} ficam de fora por
     * decisão: a atomicidade de {@code OrderPlacementService} pertence ao fluxo da aplicação, e trocá-la
     * por dezenas de {@code @Bean} seria cerimônia sem benefício.
     */
    @ArchTest
    static final ArchRule application_is_free_of_delivery_technology = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.fasterxml.jackson..",
                    "jakarta.persistence..",
                    "jakarta.mail..",
                    "feign..",
                    "org.apache.kafka..",
                    "org.springframework.data..",
                    "org.springframework.http..",
                    "org.springframework.web.."
            )
            .because("the application layer orchestrates ports; which library speaks JSON, SMTP, SQL or HTTP is an adapter's business");

    /**
     * O UUIDv7 não é detalhe substituível: o id da linha de outbox é o header {@code event-id} publicado e
     * vira o {@code causation-id} a jusante, e a ordenação temporal dele é o que mantém local o índice
     * usado pelo {@code ORDER BY created_at, id} da reivindicação. Trocá-lo por v4 degradaria as duas
     * coisas em silêncio.
     * <p>
     * Um pacote só admite a biblioteca, para que a escolha continue visível e auditável em vez de se
     * espalhar como import qualquer.
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

    // ---------------------------------------------------------------- adapters

    @ArchTest
    static final ArchRule rest_controllers_live_in_the_inbound_rest_adapter = classes()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .or().areAnnotatedWith("org.springframework.web.bind.annotation.RestControllerAdvice")
            .should().resideInAPackage(ADAPTERS_IN_REST)
            .because("HTTP is a delivery mechanism, and delivery mechanisms are inbound adapters");

    @ArchTest
    static final ArchRule kafka_listeners_live_in_the_inbound_messaging_adapter = methods()
            .that().areAnnotatedWith("org.springframework.kafka.annotation.KafkaListener")
            .should().beDeclaredInClassesThat().resideInAPackage(ADAPTERS_IN_MESSAGING)
            .because("a consumer is driven by the broker: same boundary as a controller, different transport");

    @ArchTest
    static final ArchRule jpa_entities_live_in_the_persistence_adapter = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAPackage(ADAPTERS_OUT_PERSISTENCE)
            .because("an entity is a persistence detail, not a shape the application is allowed to see");

    @ArchTest
    static final ArchRule spring_data_repositories_live_in_the_persistence_adapter = classes()
            .that().areAssignableTo("org.springframework.data.repository.Repository")
            .should().resideInAPackage(ADAPTERS_OUT_PERSISTENCE)
            .because("Spring Data interfaces implement no port: they are the driven side of one");

    @ArchTest
    static final ArchRule scheduled_methods_live_in_the_inbound_scheduler_adapter = methods()
            .that().areAnnotatedWith("org.springframework.scheduling.annotation.Scheduled")
            .should().beDeclaredInClassesThat().resideInAPackage(ADAPTERS_IN_SCHEDULER)
            .because("the clock drives the application just as the broker and HTTP do");

    @ArchTest
    static final ArchRule kafka_publishing_lives_in_the_outbound_messaging_adapter = noClasses()
            .that().resideOutsideOfPackages(ADAPTERS_OUT_MESSAGING, CONFIG)
            .should().dependOnClassesThat().resideInAPackage("org.springframework.kafka.core..")
            .because("only the adapter that implements the publisher port has business holding a KafkaTemplate");

    @ArchTest
    static final ArchRule mail_sending_lives_in_the_outbound_notification_adapter = noClasses()
            .that().resideOutsideOfPackage(ADAPTERS_OUT_NOTIFICATION)
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.mail..", "jakarta.mail..")
            .because("SMTP is one way of notifying; the port that names the intention must not know which");

    @ArchTest
    static final ArchRule feign_clients_live_in_the_outbound_client_adapter = classes()
            .that().areAnnotatedWith("org.springframework.cloud.openfeign.FeignClient")
            .should().resideInAPackage(ADAPTERS_OUT_CLIENT)
            .because("a declarative HTTP client is a driven adapter, whatever the package it grew in");
}

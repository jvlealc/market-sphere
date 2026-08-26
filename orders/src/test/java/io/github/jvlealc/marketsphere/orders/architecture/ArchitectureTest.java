package io.github.jvlealc.marketsphere.orders.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Impõe as fronteiras hexagonais deste módulo.
 */
@AnalyzeClasses(packages = ArchitectureTest.ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String ROOT_PACKAGE = "io.github.jvlealc.marketsphere.orders";

    private static final String DOMAIN = "..orders.domain..";
    private static final String APPLICATION = "..orders.application..";
    private static final String APPLICATION_MODEL = "..orders.application.model..";
    private static final String APPLICATION_EXCEPTION = "..orders.application.exception..";
    private static final String APPLICATION_IDENTITY = "..orders.application.identity..";
    private static final String APPLICATION_MESSAGING = "..orders.application.messaging..";
    private static final String APPLICATION_PORTS = "..orders.application.ports..";
    private static final String INFRASTRUCTURE = "..orders.infrastructure..";

    private static final String CONFIG = "..orders.infrastructure.config..";

    private static final String ADAPTERS_IN_REST = "..orders.infrastructure.adapters.in.rest..";
    private static final String ADAPTERS_IN_MESSAGING = "..orders.infrastructure.adapters.in.messaging..";
    private static final String ADAPTERS_IN_SCHEDULER = "..orders.infrastructure.adapters.in.scheduler..";
    private static final String ADAPTERS_OUT_PERSISTENCE = "..orders.infrastructure.adapters.out.persistence..";
    private static final String ADAPTERS_OUT_MESSAGING = "..orders.infrastructure.adapters.out.messaging..";
    private static final String ADAPTERS_OUT_NOTIFICATION = "..orders.infrastructure.adapters.out.notification..";
    private static final String ADAPTERS_OUT_CLIENT = "..orders.infrastructure.adapters.out.client..";

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
     * Lista de permitidos
     */
    @ArchTest
    static final ArchRule application_model_is_inert = classes()
            .that().resideInAPackage(APPLICATION_MODEL)
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "java..",
                    APPLICATION_MODEL,
                    APPLICATION_EXCEPTION,
                    APPLICATION_IDENTITY,
                    APPLICATION_MESSAGING,
                    DOMAIN
            )
            .because("a model package holds inert values: no ports, no container, no third-party library");

    @ArchTest
    static final ArchRule application_is_free_of_delivery_technology = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.fasterxml.jackson..",
                    "jakarta.persistence..",
                    "jakarta.mail..",
                    "feign..",
                    "org.apache.kafka.."
            )
            .because("the application layer orchestrates ports; which library speaks JSON, SMTP or SQL is an adapter's business");

    @ArchTest
    static final ArchRule ports_are_interfaces_named_port = classes()
            .that().resideInAPackage(APPLICATION_PORTS)
            .should().beInterfaces()
            .andShould().haveSimpleNameEndingWith("Port")
            .because("a port is a contract; anything with a body in that package is an adapter in disguise");

    @ArchTest
    static final ArchRule use_cases_are_named_use_case = classes()
            .that().resideInAPackage("..orders.application.usecase..")
            .should().haveSimpleNameEndingWith("UseCase")
            .because("the inbound boundary should be recognisable by name alone");

    @ArchTest
    static final ArchRule no_field_injection = noFields()
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .orShould().beAnnotatedWith("org.springframework.beans.factory.annotation.Value")
            .because("constructor injection makes dependencies explicit and the class usable without a container");

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

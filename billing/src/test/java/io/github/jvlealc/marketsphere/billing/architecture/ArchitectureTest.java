package io.github.jvlealc.marketsphere.billing.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Impõe as fronteiras hexagonais deste módulo.
 */
@AnalyzeClasses(packages = ArchitectureTest.ROOT_PACKAGE)
class ArchitectureTest {

    static final String ROOT_PACKAGE = "io.github.jvlealc.marketsphere.billing";

    private static final String DOMAIN = "..billing.domain..";
    private static final String APPLICATION = "..billing.application..";
    private static final String APPLICATION_MODEL = "..billing.application.model..";
    private static final String APPLICATION_EXCEPTION = "..billing.application.exception..";
    private static final String APPLICATION_IDENTITY = "..billing.application.identity..";
    private static final String APPLICATION_PORTS = "..billing.application.ports..";
    private static final String INFRASTRUCTURE = "..billing.infrastructure..";

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
                    "org.apache.kafka.."
            )
            .because("Invoice must stay testable with plain new(), with no container and no annotation processor");

    @ArchTest
    static final ArchRule application_does_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
            .because("adapters are chosen at wiring time; the application only knows its own ports");

    /**
     * Lista de permitidos, e não de proibidos: uma lista de proibidos só enxerga o que já se sabia
     * procurar, enquanto esta faz qualquer biblioteca nova dentro do modelo falhar o build no dia em que
     * entra.
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
                    "io.minio..",
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
            .that().resideInAPackage("..billing.application.usecase..")
            .should().haveSimpleNameEndingWith("UseCase")
            .because("the inbound boundary should be recognisable by name alone");

    @ArchTest
    static final ArchRule no_field_injection = noFields()
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .because("constructor injection makes dependencies explicit and the class usable without a container");
}

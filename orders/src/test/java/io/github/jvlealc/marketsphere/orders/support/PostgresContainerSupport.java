package io.github.jvlealc.marketsphere.orders.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Um único PostgreSQL para todos os testes de integração do módulo.
 * <p>
 * O schema é aplicado uma vez, na criação do container.
 */
public abstract class PostgresContainerSupport {

    @SuppressWarnings("resource")
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6")
            .withInitScript("db/schema.sql");

    static {
        POSTGRES.start();
    }
}

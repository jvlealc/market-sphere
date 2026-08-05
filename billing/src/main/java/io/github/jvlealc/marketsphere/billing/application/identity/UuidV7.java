package io.github.jvlealc.marketsphere.billing.application.identity;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

import java.util.UUID;

/**
 * Gera identificadores UUIDv7 (RFC 9562), ordenáveis por tempo de criação.
 * <p>
 * O v7 é escolha de desempenho de índice: o v4 é aleatório e espalha inserções pelo B-tree da chave
 * primária, o que numa outbox — insert-heavy e em crescimento contínuo — custa divisão de página e
 * amplificação de WAL. O timestamp de criação fica legível a partir do ID; restam 62 bits aleatórios.
 */
public final class UuidV7 {

    /** Seguro para uso concorrente: consumidor Kafka e worker da outbox geram IDs em threads distintas. */
    private static final TimeBasedEpochGenerator GENERATOR = Generators.timeBasedEpochGenerator();

    private UuidV7() {
    }

    public static UUID generate() {
        return GENERATOR.generate();
    }
}

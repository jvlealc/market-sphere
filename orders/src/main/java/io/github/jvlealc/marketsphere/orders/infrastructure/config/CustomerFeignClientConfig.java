package io.github.jvlealc.marketsphere.orders.infrastructure.config;

import feign.RequestInterceptor;
import io.github.jvlealc.marketsphere.orders.infrastructure.config.props.CustomerClientProps;
import org.springframework.context.annotation.Bean;

/**
 * Configuração do cliente Feign de {@code customers}.
 *
 * <p><strong>Não anotar com {@code @Configuration} nem {@code @Component}.</strong> É a ausência de
 * estereótipo que mantém esta classe fora do component scan; o Spring Cloud a registra no contexto filho
 * deste cliente por causa do atributo {@code configuration} do {@code @FeignClient}. Anotada, o
 * {@link RequestInterceptor} viraria bean global e a chave do {@code customers} passaria a viajar também
 * nas chamadas ao {@code products}.
 */
public class CustomerFeignClientConfig {

    @Bean
    RequestInterceptor customerApiKeyInterceptor(CustomerClientProps props) {
        return template -> template.header("X-Internal-Service-Auth", props.apiKey());
    }
}

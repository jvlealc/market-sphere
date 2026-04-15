package io.github.jvlealc.marketsphere.customers;

import io.github.jvlealc.marketsphere.customers.client.BrasilApiClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(
        basePackages = "io.github.jvlealc.marketsphere.customers.client",
        clients = BrasilApiClient.class
)
public class CustomersApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomersApplication.class, args);
	}

}

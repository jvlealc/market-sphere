package io.github.jvlealc.marketsphere.orders.interfaces.rest.common.support;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

public class HeaderLocationBuilder {

    public static URI build(Long orderId) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{order-id}")
                .buildAndExpand(orderId)
                .toUri();
    }
}

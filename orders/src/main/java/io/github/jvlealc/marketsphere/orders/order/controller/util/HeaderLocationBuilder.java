package io.github.jvlealc.marketsphere.orders.order.controller.util;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

public class HeaderLocationBuilder {

    public static URI build(Long id) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}

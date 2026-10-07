package fi.vjh.domain;

import io.micronaut.serde.annotation.Serdeable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Serdeable
public record Order(
        Long id,
        Long cartId,
        Date orderPlaced,
        Integer quantity,
        String status,
        List<OrderItem> items
) {

    public Order(Long id, Long cartId, Integer quantity, String status) {
        this(id, cartId, null, quantity, status, new ArrayList<>());
    }

}

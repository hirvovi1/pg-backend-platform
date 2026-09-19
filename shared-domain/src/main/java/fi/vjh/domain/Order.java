package fi.vjh.domain;

import io.micronaut.serde.annotation.Serdeable;

import java.util.Date;
import java.util.List;

@Serdeable
public record Order(
        Long id,
        Long productId,
        Date orderPlaced,
        Integer quantity,
        String status,
        List<OrderItem> items
) {

    public Order(Long id, Long productId, Integer quantity, String status) {
        this(id, productId, null, quantity, status, List.of());
    }
}

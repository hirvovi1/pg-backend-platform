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

    public static Order from(Order order, Date orderPlaced) {
        return new Order(order.id, order.productId, orderPlaced, order.quantity, order.status, order.items);
    }

}

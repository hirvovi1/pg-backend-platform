package fi.vjh.domain;

import io.micronaut.serde.annotation.Serdeable;

import java.util.Date;
import java.util.List;

@Serdeable
public record Cart(
        Long id,
        Long productId,
        Date cartCreated,
        Integer quantity,
        String status,
        List<CartItem> items
) {

    public Cart(Long id, Long productId, Integer quantity, String status) {
        this(id, productId, null, quantity, status, List.of());
    }

    public static Cart from(Cart cart, Date cartCreated) {
        return new Cart(cart.id, cart.productId, cartCreated, cart.quantity, cart.status, cart.items);
    }

}

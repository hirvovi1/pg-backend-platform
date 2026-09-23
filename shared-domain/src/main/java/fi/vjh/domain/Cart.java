package fi.vjh.domain;

import io.micronaut.serde.annotation.Serdeable;

import java.util.Date;
import java.util.List;

@Serdeable
public record Cart(
        Long id,
        Date cartCreated,
        String status,
        List<CartItem> items
) {

    public Cart(Long id, String status) {
        this(id, null, status, List.of());
    }

    public static Cart from(Cart cart, Date cartCreated) {
        return new Cart(cart.id, cartCreated, cart.status, cart.items);
    }

}

package fi.vjh.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Creator;
import io.micronaut.serde.annotation.Serdeable;

import java.util.Date;
import java.util.List;

@Serdeable
@JsonInclude(JsonInclude.Include.ALWAYS)
public record Cart(
        Long id,
        Date cartCreated,
        String status,
        List<CartItem> items
) {
    @Creator
    public Cart {
        if (items == null) {
            items = List.of();
        }
    }
    public Cart(Long id, String status) {
        this(id, null, status, List.of());
    }
}

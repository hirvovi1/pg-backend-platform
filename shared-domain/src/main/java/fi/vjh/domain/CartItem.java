package fi.vjh.domain;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record CartItem(
        Long id,
        Integer itemCount,
        Long productId,
        String productName,
        Long priceInCents
) {
}

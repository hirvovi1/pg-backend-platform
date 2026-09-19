package fi.vjh.domain;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record OrderItem(
        Long id,
        Integer itemCount,
        Long productId
) {
}

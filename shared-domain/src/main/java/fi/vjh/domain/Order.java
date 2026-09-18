package fi.vjh.domain;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record Order(
        Long id,
        Long productId,
        Integer quantity,
        String status
) {
}

package fi.vjh.domain;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record Product(
        Long id,
        String name,
        String description,
        Long priceInCents,
        String imageUrl,
        ProductStatus status
) {}


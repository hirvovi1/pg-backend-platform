package fi.vjh.domain;

import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;

@Serdeable
public record Product(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        ProductStatus status
) {}


package fi.vjh.repository;

import fi.vjh.domain.ProductStatus;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "products")
@Serdeable
public class ProductRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private Long priceInCents;
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

}

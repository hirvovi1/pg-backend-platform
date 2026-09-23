package fi.vjh.repository;

import fi.vjh.domain.CartStatus;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "carts")
@Serdeable
public class CartRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer quantity;
    private Date cartCreated;
    @OneToMany(
            mappedBy = "cart",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<CartItemRow> items;

    @Enumerated(EnumType.STRING)
    private CartStatus status;

}

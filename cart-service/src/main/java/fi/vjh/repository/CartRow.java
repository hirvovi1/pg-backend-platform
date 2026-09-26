package fi.vjh.repository;

import fi.vjh.domain.CartStatus;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "carts")
@Serdeable
public class CartRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Date cartCreated;
    @OneToMany(
            mappedBy = "cart",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<CartItemRow> items;

    public List<CartItemRow> getItems() {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        return this.items;
    }

    @Enumerated(EnumType.STRING)
    private CartStatus status = CartStatus.PENDING;

}

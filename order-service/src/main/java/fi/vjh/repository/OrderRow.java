package fi.vjh.repository;

import fi.vjh.domain.OrderStatus;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "orders")
@Serdeable
public class OrderRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long cartId;
    private Integer quantity;

    @CreationTimestamp
    private Date orderPlaced;

    @OneToMany(
            mappedBy = "order",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItemRow> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    public void addItem(OrderItemRow item) {
        this.items.add(item);
        item.setOrder(this);
    }
}

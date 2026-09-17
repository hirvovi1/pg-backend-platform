package fi.vjh.repository;

import fi.vjh.domain.Order;
import fi.vjh.domain.OrderStatus;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByProductId(Long productId);

    List<Order> findByProductIdAndStatusIn(Long productId, List<OrderStatus> statuses);
}

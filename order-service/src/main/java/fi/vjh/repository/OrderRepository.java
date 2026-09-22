package fi.vjh.repository;

import fi.vjh.domain.OrderStatus;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderRow, Long> {

    List<OrderRow> findByProductId(Long productId);

    List<OrderRow> findByProductIdAndStatusIn(Long productId, List<OrderStatus> statuses);

}

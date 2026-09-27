package fi.vjh.repository;

import fi.vjh.domain.OrderStatus;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderRow, Long> {

    @Query("DELETE FROM OrderItemRow")
    void deleteAllOrderItemsBulk();

    @Query("DELETE FROM OrderRow")
    void deleteAllOrdersBulk();

    List<OrderRow> findByItemsProductIdAndStatusIn(Long productId, List<OrderStatus> statuses);

}

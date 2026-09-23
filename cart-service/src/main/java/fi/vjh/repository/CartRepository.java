package fi.vjh.repository;

import fi.vjh.domain.CartStatus;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<CartRow, Long> {

    List<CartRow> findByItemsProductId(Long productId);

    List<CartRow> findByItemsProductIdAndStatusIn(Long productId, List<CartStatus> statuses);

}

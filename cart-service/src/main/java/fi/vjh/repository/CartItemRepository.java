package fi.vjh.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemRow, Long> {
}

package fi.vjh.repository;

import fi.vjh.domain.ProductStatus;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductRow, Long> {

    List<ProductRow> findByStatus(ProductStatus status);

}

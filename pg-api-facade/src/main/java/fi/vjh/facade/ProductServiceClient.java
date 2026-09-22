package fi.vjh.facade;

import fi.vjh.domain.Product;

import java.util.List;

public interface ProductServiceClient {

    List<Product> getProducts();

    Product getProductById(Long id);

    Product addProduct(Product product);

    void deleteProduct(Long id);
}

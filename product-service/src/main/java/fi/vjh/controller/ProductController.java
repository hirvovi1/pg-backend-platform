package fi.vjh.controller;

import fi.vjh.domain.Product;
import fi.vjh.domain.ProductStatus;
import fi.vjh.repository.ProductRepository;
import fi.vjh.service.OrderServicePort;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import java.util.List;

@Controller("/products")
@ExecuteOn(TaskExecutors.BLOCKING)
public class ProductController {

    private final ProductRepository productRepository;
    private final OrderServicePort orderServicePort;

    public ProductController(ProductRepository productRepository, OrderServicePort orderServicePort) {
        this.productRepository = productRepository;
        this.orderServicePort = orderServicePort;
    }

    @Get
    public List<Product> getAllActiveProducts() {
        return productRepository.findByStatus(ProductStatus.ACTIVE);
    }

    @Get("/{id}")
    public HttpResponse<Product> getProductById(Long id) {
        return productRepository.findById(id)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Post
    public HttpResponse<Product> addProduct(@Body Product product) {
        product.setStatus(ProductStatus.ACTIVE); // Varmistetaan että uusi tuote on aktiivinen
        Product saved = productRepository.save(product);
        return HttpResponse.created(saved);
    }

    @Delete("/{id}")
    public HttpResponse<Void> softDeleteProduct(Long id) {
        return productRepository.findById(id)
                .map(product -> {
                    if (orderServicePort.productHasOpenOrders(id)) {
                        return HttpResponse.<Void>status(HttpStatus.CONFLICT);
                    }
                    product.setStatus(ProductStatus.ARCHIVED);
                    productRepository.update(product);
                    return HttpResponse.<Void>noContent();
                })
                .orElse(HttpResponse.notFound());
    }
}

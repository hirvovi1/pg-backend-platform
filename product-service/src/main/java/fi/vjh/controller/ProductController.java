package fi.vjh.controller;

import fi.vjh.domain.Product;
import fi.vjh.repository.ProductRow;
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
        return productRepository.findByStatus(ProductStatus.ACTIVE).stream()
                .map(this::convertToProduct)
                .toList();
    }

    @Get("/{id}")
    public HttpResponse<Product> getProductById(Long id) {
        return productRepository.findById(id)
                .map(this::convertToProduct)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    private Product convertToProduct(ProductRow row) {
        return new Product(
                row.getId(),
                row.getName(),
                row.getDescription(),
                row.getPrice(),
                row.getImageUrl(),
                row.getStatus()
        );
    }

    @Post
    public HttpResponse<Product> addProduct(@Body Product product) {
        ProductRow productRow = convertToProductRow(product);
        productRow.setStatus(ProductStatus.ACTIVE);
        ProductRow saved = productRepository.save(productRow);
        return HttpResponse.created(convertToProduct(saved));
    }

    private ProductRow convertToProductRow(Product product) {
        ProductRow row = new ProductRow();
        row.setId(product.id());
        row.setName(product.name());
        row.setDescription(product.description());
        row.setPrice(product.price());
        row.setImageUrl(product.imageUrl());
        row.setStatus(product.status());
        return row;
    }

    @Delete("/{id}")
    public HttpResponse<Void> softDeleteProduct(Long id) {
        return productRepository.findById(id)
                .map(productRow -> {
                    if (orderServicePort.productHasOpenOrders(id)) {
                        return HttpResponse.<Void>status(HttpStatus.CONFLICT);
                    }
                    productRow.setStatus(ProductStatus.ARCHIVED);
                    productRepository.update(productRow);
                    return HttpResponse.<Void>noContent();
                })
                .orElse(HttpResponse.notFound());
    }
}

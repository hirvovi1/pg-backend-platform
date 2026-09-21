package fi.vjh.facade.impl;

import fi.vjh.domain.Product;
import fi.vjh.facade.ProductServiceClient;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class DefaultProductServiceClient implements ProductServiceClient {

    private final HttpClient client;

    public DefaultProductServiceClient(@Client("http://product-service:8082") HttpClient client) {
        this.client = client;
    }

    @Override
    public List<Product> getProducts() {
        return client.toBlocking().retrieve(HttpRequest.GET("/products"), Argument.listOf(Product.class));
    }

    @Override
    public Product getProductById(Long id) {
        return client.toBlocking().retrieve(HttpRequest.GET("/products/" + id), Product.class);
    }

    @Override
    public Product addProduct(Product product) {
        return client.toBlocking().retrieve(HttpRequest.POST("/products", product), Product.class);
    }

    @Override
    public void deleteProduct(Long id) {
        client.toBlocking().exchange(HttpRequest.DELETE("/products/" + id));
    }
}

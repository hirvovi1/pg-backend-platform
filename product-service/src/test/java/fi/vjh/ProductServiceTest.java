package fi.vjh;

import fi.vjh.domain.Product;
import fi.vjh.domain.ProductStatus;
import fi.vjh.repository.ProductRepository;
import fi.vjh.service.OrderServicePort;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Singleton;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(transactional = false)
class ProductServiceTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ProductRepository productRepository;

    @Inject
    DataInitializer dataInitializer;

    @Inject
    TestOrderServicePort orderServicePort;

    @BeforeEach
    void cleanDatabase() {
        productRepository.deleteAll();
        orderServicePort.setProductHasOpenOrders(false);
    }

    @Test
    void getProductsReturnsOnlyActiveProducts() {
        Product active = saveProduct("Active product", ProductStatus.ACTIVE);
        saveProduct("Archived product", ProductStatus.ARCHIVED);

        List<Product> products = client.toBlocking().retrieve(
                HttpRequest.GET("/products"),
                Argument.listOf(Product.class)
        );

        assertEquals(1, products.size());
        assertEquals(active.getId(), products.get(0).getId());
        assertEquals(ProductStatus.ACTIVE, products.get(0).getStatus());
    }

    @Test
    void createProductForcesActiveStatus() {
        Product request = product("New product", ProductStatus.ARCHIVED);

        HttpResponse<Product> response = client.toBlocking().exchange(
                HttpRequest.POST("/products", request),
                Product.class
        );

        assertEquals(201, response.getStatus().getCode());
        Product saved = response.body();
        assertNotNull(saved.getId());
        assertEquals("New product", saved.getName());
        assertEquals(ProductStatus.ACTIVE, saved.getStatus());
    }

    @Test
    void getProductByIdReturnsProductWhenItExists() {
        Product product = saveProduct("Find me", ProductStatus.ACTIVE);

        HttpResponse<Product> response = client.toBlocking().exchange(
                HttpRequest.GET("/products/" + product.getId()),
                Product.class
        );

        assertEquals(200, response.getStatus().getCode());
        assertEquals(product.getId(), response.body().getId());
        assertEquals("Find me", response.body().getName());
    }

    @Test
    void getProductByIdReturnsNotFoundForUnknownProduct() {
        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().retrieve(HttpRequest.GET("/products/999999"))
        );

        assertEquals(404, exception.getStatus().getCode());
    }

    @Test
    void deleteProductArchivesItAndRemovesItFromActiveListing() {
        Product product = saveProduct("Archive me", ProductStatus.ACTIVE);

        HttpResponse<?> deleteResponse = client.toBlocking().exchange(
                HttpRequest.DELETE("/products/" + product.getId()),
                Argument.of(String.class)
        );

        assertEquals(204, deleteResponse.getStatus().getCode());
        Product archived = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(ProductStatus.ARCHIVED, archived.getStatus());

        List<Product> activeProducts = client.toBlocking().retrieve(
                HttpRequest.GET("/products"),
                Argument.listOf(Product.class)
        );
        assertTrue(activeProducts.stream().noneMatch(item -> product.getId().equals(item.getId())));
    }

    @Test
    void deleteUnknownProductReturnsNotFound() {
        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.DELETE("/products/999999"))
        );

        assertEquals(404, exception.getStatus().getCode());
    }

    @Test
    void deleteProductWithPendingOrderReturnsConflictAndKeepsItActive() {
        Product product = saveProduct("Order pending", ProductStatus.ACTIVE);
        orderServicePort.setProductHasOpenOrders(true);

        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.DELETE("/products/" + product.getId()))
        );

        assertEquals(409, exception.getStatus().getCode());
        Product unchanged = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(ProductStatus.ACTIVE, unchanged.getStatus());
    }


    @Test
    void dataInitializerSeedsProductsWhenRepositoryIsEmpty() {
        dataInitializer.onStartup(null);

        List<Product> products = productRepository.findAll();

        assertEquals(2, products.size());
        Product coffee = products.stream()
                .filter(product -> "Koodauskahvi".equals(product.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals("Tumma paahto, pitää bugit loitolla.", coffee.getDescription());
        assertEquals(new BigDecimal("12.50"), coffee.getPrice());
        assertEquals("https://example.com", coffee.getImageUrl());
        assertEquals(ProductStatus.ACTIVE, coffee.getStatus());

        Product shirt = products.stream()
                .filter(product -> "Micronaut t-paita".equals(product.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals("Nopeampi käynnistymisaika kuin puuvillalla yleensä.", shirt.getDescription());
        assertEquals(new BigDecimal("25.00"), shirt.getPrice());
        assertEquals("https://example.com", shirt.getImageUrl());
        assertEquals(ProductStatus.ACTIVE, shirt.getStatus());
    }

    @Test
    void dataInitializerDoesNotAddProductsWhenRepositoryIsNotEmpty() {
        Product existing = saveProduct("Existing product", ProductStatus.ARCHIVED);

        dataInitializer.onStartup(null);

        List<Product> products = productRepository.findAll();

        assertEquals(1, products.size());
        assertEquals(existing.getId(), products.get(0).getId());
        assertEquals("Existing product", products.get(0).getName());
        assertEquals(ProductStatus.ARCHIVED, products.get(0).getStatus());
    }

    private Product saveProduct(String name, ProductStatus status) {
        return productRepository.save(product(name, status));
    }

    private Product product(String name, ProductStatus status) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Test description");
        product.setPrice(new BigDecimal("10.00"));
        product.setImageUrl("https://example.com/product");
        product.setStatus(status);
        return product;
    }

    @Singleton
    @Replaces(OrderServicePort.class)
    static class TestOrderServicePort implements OrderServicePort {

        private boolean productHasOpenOrders;

        void setProductHasOpenOrders(boolean productHasOpenOrders) {
            this.productHasOpenOrders = productHasOpenOrders;
        }

        @Override
        public boolean productHasOpenOrders(Long productId) {
            return productHasOpenOrders;
        }
    }

}

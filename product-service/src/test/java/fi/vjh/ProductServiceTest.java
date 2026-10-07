package fi.vjh;

import fi.vjh.domain.Product;
import fi.vjh.domain.ProductStatus;
import fi.vjh.repository.ProductRepository;
import fi.vjh.repository.ProductRow;
import fi.vjh.service.OrderServicePort;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@Property(name = "micronaut.server.port", value = "-1")
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
        ProductRow active = saveProduct("Active product", ProductStatus.ACTIVE);
        saveProduct("Archived product", ProductStatus.ARCHIVED);

        List<Product> products = client.toBlocking().retrieve(
                HttpRequest.GET("/products"),
                Argument.listOf(Product.class)
        );

        assertEquals(1, products.size());
        assertEquals(active.getId(), products.get(0).id());
        assertEquals(ProductStatus.ACTIVE, products.get(0).status());
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
        assertNotNull(saved.id());
        assertEquals("New product", saved.name());
        assertEquals(ProductStatus.ACTIVE, saved.status());
    }

    @Test
    void getProductByIdReturnsProductWhenItExists() {
        ProductRow productRow = saveProduct("Find me", ProductStatus.ACTIVE);

        HttpResponse<Product> response = client.toBlocking().exchange(
                HttpRequest.GET("/products/" + productRow.getId()),
                Product.class
        );

        assertEquals(200, response.getStatus().getCode());
        assertEquals(productRow.getId(), response.body().id());
        assertEquals("Find me", response.body().name());
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
    void getProductByIdRejectsNonNumericId() {
        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().retrieve(HttpRequest.GET("/products/not-a-number"))
        );

        assertEquals(400, exception.getStatus().getCode());
    }

    @Test
    void deleteProductArchivesItAndRemovesItFromActiveListing() {
        ProductRow productRow = saveProduct("Archive me", ProductStatus.ACTIVE);

        HttpResponse<?> deleteResponse = client.toBlocking().exchange(
                HttpRequest.DELETE("/products/" + productRow.getId()),
                Argument.of(String.class)
        );

        assertEquals(204, deleteResponse.getStatus().getCode());
        ProductRow archived = productRepository.findById(productRow.getId()).orElseThrow();
        assertEquals(ProductStatus.ARCHIVED, archived.getStatus());

        List<Product> activeProducts = client.toBlocking().retrieve(
                HttpRequest.GET("/products"),
                Argument.listOf(Product.class)
        );
        assertTrue(activeProducts.stream().noneMatch(item -> productRow.getId().equals(item.id())));
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
        ProductRow productRow = saveProduct("Order pending", ProductStatus.ACTIVE);
        orderServicePort.setProductHasOpenOrders(true);

        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.DELETE("/products/" + productRow.getId()))
        );

        assertEquals(409, exception.getStatus().getCode());
        ProductRow unchanged = productRepository.findById(productRow.getId()).orElseThrow();
        assertEquals(ProductStatus.ACTIVE, unchanged.getStatus());
    }


    @Test
    void dataInitializerSeedsProductsWhenRepositoryIsEmpty() {
        dataInitializer.onStartup(null);

        List<ProductRow> productRows = productRepository.findAll();

        assertEquals(2, productRows.size());
        ProductRow coffee = productRows.stream()
                .filter(productRow -> "Koodauskahvi".equals(productRow.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals("Tumma paahto, pitää bugit loitolla.", coffee.getDescription());
        assertEquals(1250L, coffee.getPriceInCents());
        assertEquals("https://example.com", coffee.getImageUrl());
        assertEquals(ProductStatus.ACTIVE, coffee.getStatus());

        ProductRow shirt = productRows.stream()
                .filter(productRow -> "Micronaut t-paita".equals(productRow.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals("Nopeampi käynnistymisaika kuin puuvillalla yleensä.", shirt.getDescription());
        assertEquals(2500L, shirt.getPriceInCents());
        assertEquals("https://example.com", shirt.getImageUrl());
        assertEquals(ProductStatus.ACTIVE, shirt.getStatus());
    }

    @Test
    void dataInitializerDoesNotAddProductsWhenRepositoryIsNotEmpty() {
        ProductRow existing = saveProduct("Existing product", ProductStatus.ARCHIVED);

        dataInitializer.onStartup(null);

        List<ProductRow> productRows = productRepository.findAll();

        assertEquals(1, productRows.size());
        assertEquals(existing.getId(), productRows.get(0).getId());
        assertEquals("Existing product", productRows.get(0).getName());
        assertEquals(ProductStatus.ARCHIVED, productRows.get(0).getStatus());
    }

    private ProductRow saveProduct(String name, ProductStatus status) {
        return productRepository.save(productRow(name, status));
    }

    private Product product(String name, ProductStatus status) {
        return new Product(
                null,
                name,
                "Test description",
                1000L,
                "https://example.com/product",
                status
        );
    }

    private ProductRow productRow(String name, ProductStatus status) {
        ProductRow productRow = new ProductRow();
        productRow.setName(name);
        productRow.setDescription("Test description");
        productRow.setPriceInCents(1000L);
        productRow.setImageUrl("https://example.com/product");
        productRow.setStatus(status);
        return productRow;
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

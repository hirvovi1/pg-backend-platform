package fi.vjh;

import fi.vjh.domain.Cart;
import fi.vjh.domain.Order;
import fi.vjh.domain.Product;
import fi.vjh.domain.ProductStatus;
import fi.vjh.facade.CartServiceClient;
import fi.vjh.facade.CurrencyServiceClient;
import fi.vjh.facade.OrderServiceClient;
import fi.vjh.facade.ProductServiceClient;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(transactional = false)
@Property(name = "micronaut.server.port", value = "-1")
class FacadeTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void delegatesProductRequests() {
        var response = client.toBlocking().exchange(
                HttpRequest.GET("/api/v1/frontend/products"),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(
                "[{\"id\":1,\"name\":\"Coffee\",\"description\":\"Beans\",\"price\":9.99,\"imageUrl\":\"coffee.png\",\"status\":\"ACTIVE\"}]",
                response.body()
        );
    }

    @Test
    void delegatesOrderCreation() {
        var order = new Order(7L, 1L, 2, "PENDING");
        var response = client.toBlocking().exchange(
                HttpRequest.POST("/api/v1/frontend/orders", order)
                        .contentType(MediaType.APPLICATION_JSON),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(
                "{\"id\":7,\"productId\":1,\"quantity\":2,\"status\":\"PENDING\"}",
                response.body()
        );
    }

    @Test
    void delegatesCurrencyConversion() {
        var response = client.toBlocking().exchange(
                HttpRequest.GET("/api/v1/frontend/currency/convert?amountInCents=100"),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.body().contains("\"convertedAmount\":\"1.08\""));
        assertTrue(response.body().contains("\"currency\":\"USD\""));
    }

    @Test
    void delegatesProductOpenOrderCheck() {
        var response = client.toBlocking().exchange(
                HttpRequest.GET("/api/v1/frontend/orders/product/1/has-open-orders"),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals("true", response.body());
    }

    @Test
    void delegatesCartCreation() {
        var cart = new Cart(8L, 2, "PENDING");
        var response = client.toBlocking().exchange(
                HttpRequest.POST("/api/v1/frontend/carts", cart)
                        .contentType(MediaType.APPLICATION_JSON),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(
                "{\"id\":8,\"quantity\":2,\"status\":\"PENDING\"}",
                response.body()
        );
    }

    @Test
    void delegatesProductOpenCartCheck() {
        var response = client.toBlocking().exchange(
                HttpRequest.GET("/api/v1/frontend/carts/product/1/has-open-carts"),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals("true", response.body());
    }

    @Factory
    static class DownstreamClientMocks {

        private final static Cart cart = new Cart(1L, 1, "STATUS");

        @Singleton
        @Replaces(ProductServiceClient.class)
        ProductServiceClient productServiceClient() {
            return new ProductServiceClient() {
                @Override
                public List<Product> getProducts() {
                    return List.of(new Product(
                            1L,
                            "Coffee",
                            "Beans",
                            new BigDecimal("9.99"),
                            "coffee.png",
                            ProductStatus.ACTIVE
                    ));
                }

                @Override
                public Product getProductById(Long id) {
                    return getProducts().getFirst();
                }

                @Override
                public Product addProduct(Product product) {
                    return product;
                }

                @Override
                public void deleteProduct(Long id) {
                }
            };
        }

        @Singleton
        @Replaces(OrderServiceClient.class)
        OrderServiceClient orderServiceClient() {
            return new OrderServiceClient() {
                @Override
                public List<Order> getOrders() {
                    return List.of();
                }

                @Override
                public List<Order> getOrdersForProduct(Long productId) {
                    return List.of();
                }

                @Override
                public boolean productHasOpenOrders(Long productId) {
                    return true;
                }

                @Override
                public Order getOrderById(Long id) {
                    return new Order(id, 1L, 2, "PENDING");
                }

                @Override
                public Order addOrder(Order order) {
                    return order;
                }

                @Override
                public Order cancelOrder(Long id) {
                    return new Order(id, 1L, 2, "CANCELLED");
                }

                @Override
                public Order payOrder(Long id) {
                    return new Order(id, 1L, 2, "CONFIRMED");
                }
            };
        }

        @Singleton
        @Replaces(CurrencyServiceClient.class)
        CurrencyServiceClient currencyServiceClient() {
            return amountInCents -> Map.of(
                    "currency", "USD",
                    "convertedAmount", "1.08"
            );
        }

        @Singleton
        @Replaces(CartServiceClient.class)
        CartServiceClient cartServiceClient() {
            return new CartServiceClient() {
                @Override
                public List<Cart> getCarts() {
                    return List.of();
                }

                @Override
                public List<Cart> getCartsForProduct(Long productId) {
                    return List.of();
                }

                @Override
                public boolean productHasOpenCarts(Long productId) {
                    return true;
                }

                @Override
                public Cart getCartById(Long id) {
                    return new Cart(id, 2, "PENDING");
                }

                @Override
                public Cart saveCart(Cart cart) {
                    return cart;
                }

                @Override
                public Cart cancelCart(Long id) {
                    return new Cart(id, 2, "CANCELLED");
                }

                @Override
                public Cart payCart(Long id) {
                    return new Cart(id, 2, "CONFIRMED");
                }

                @Override
                public Cart create() {
                    return cart;
                }
            };
        }
    }
}

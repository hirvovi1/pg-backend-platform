package fi.vjh;

import fi.vjh.domain.Cart;
import fi.vjh.domain.CartItem;
import fi.vjh.domain.CartStatus;
import fi.vjh.repository.CartItemRepository;
import fi.vjh.repository.CartItemRow;
import fi.vjh.repository.CartRepository;
import fi.vjh.repository.CartRow;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@Property(name = "micronaut.server.port", value = "-1")
class CartServiceTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    CartRepository cartRepository;

    @Inject
    CartItemRepository cartItemRepository;

    @BeforeEach
    void cleanDatabase() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
    }

    @Test
    void getAllCartsReturnsCarts() {
        CartRow first = saveCart(1L, 2, CartStatus.PENDING);
        saveCart(2L, 1, CartStatus.CANCELLED);

        List<Cart> carts = client.toBlocking().retrieve(
                HttpRequest.GET("/carts"),
                Argument.listOf(Cart.class)
        );

        assertEquals(2, carts.size());
        assertTrue(carts.stream().anyMatch(cart ->
                first.getId().equals(cart.id())
                        && cart.quantity().equals(2)
                        && cart.status().equals(CartStatus.PENDING.name())
        ));
    }

    @Test
    void getCartsForProductReturnsMatchingCarts() {
        CartRow matching = saveCart(7L, 2, CartStatus.PENDING);
        saveCart(8L, 1, CartStatus.CONFIRMED);

        List<Cart> carts = client.toBlocking().retrieve(
                HttpRequest.GET("/carts/product/7"),
                Argument.listOf(Cart.class)
        );

        assertEquals(1, carts.size());
        assertEquals(matching.getId(), carts.get(0).id());
    }

    @Test
    void getCartByIdReturnsCartWhenItExists() {
        CartRow cartRow = saveCart(3L, 4, CartStatus.CONFIRMED);

        HttpResponse<Cart> response = client.toBlocking().exchange(
                HttpRequest.GET("/carts/" + cartRow.getId()),
                Cart.class
        );

        assertEquals(200, response.getStatus().getCode());
        assertEquals(cartRow.getId(), response.body().id());
        assertEquals(4, response.body().quantity());
        assertEquals(CartStatus.CONFIRMED.name(), response.body().status());
    }

    @Test
    void getCartByIdReturnsCartItems() {
        CartRow cartRow = saveCart(3L, 4, CartStatus.CONFIRMED);
        cartRow.setItems(List.of(
                item(cartRow, 11L, 2),
                item(cartRow, 12L, 1)
        ));
        cartRepository.update(cartRow);

        HttpResponse<Cart> response = client.toBlocking().exchange(
                HttpRequest.GET("/carts/" + cartRow.getId()),
                Cart.class
        );

        assertEquals(200, response.getStatus().getCode());
        assertEquals(List.of(11L, 12L), response.body().items().stream()
                .map(CartItem::productId)
                .toList());
        assertEquals(List.of(2, 1), response.body().items().stream()
                .map(CartItem::itemCount)
                .toList());
        assertTrue(response.body().items().stream().allMatch(item -> item.id() != null));
    }

    @Test
    void getUnknownCartReturnsNotFound() {
        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().retrieve(HttpRequest.GET("/carts/999999"))
        );

        assertEquals(404, exception.getStatus().getCode());
    }

    @Test
    void createCartForcesPendingStatus() {
        Cart request = new Cart(null, 3, "CANCELLED");

        HttpResponse<Cart> response = client.toBlocking().exchange(
                HttpRequest.POST("/carts", request),
                Cart.class
        );

        assertEquals(201, response.getStatus().getCode());
        Cart saved = response.body();
        assertNotNull(saved.id());
        assertEquals(3, saved.quantity());
        assertEquals(CartStatus.PENDING.name(), saved.status());
    }

    @Test
    void createCartPersistsAndReturnsCartItems() {
        Cart request = new Cart(
                null,
                null,
                3,
                "CANCELLED",
                List.of(
                        new CartItem(null, 2, 101L),
                        new CartItem(null, 1, 102L)
                )
        );

        HttpResponse<Cart> response = client.toBlocking().exchange(
                HttpRequest.POST("/carts", request),
                Cart.class
        );

        assertEquals(201, response.getStatus().getCode());
        assertEquals(
                List.of(101L, 102L),
                response.body().items().stream().map(CartItem::productId).toList()
        );
        assertEquals(
                List.of(2, 1),
                response.body().items().stream().map(CartItem::itemCount).toList()
        );

        CartRow saved = cartRepository.findById(response.body().id()).orElseThrow();
        assertEquals(2, saved.getItems().size());
        assertTrue(saved.getItems().stream().allMatch(item -> item.getCart().getId().equals(saved.getId())));
    }

    @Test
    void createCartWithInvalidStatusReturnsBadRequest() {
        Cart request = new Cart(null, 3, "INVALID");

        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST("/carts", request),
                        Cart.class
                )
        );

        assertEquals(400, exception.getStatus().getCode());
    }

    @Test
    void cancelCartChangesStatusToCancelled() {
        CartRow cartRow = saveCart(12L, 1, CartStatus.PENDING);

        HttpResponse<Cart> response = client.toBlocking().exchange(
                HttpRequest.PUT("/carts/" + cartRow.getId() + "/cancel", null),
                Cart.class
        );

        assertEquals(200, response.getStatus().getCode());
        assertEquals(CartStatus.CANCELLED.name(), response.body().status());
        assertEquals(
                CartStatus.CANCELLED,
                cartRepository.findById(cartRow.getId()).orElseThrow().getStatus()
        );
    }

    @Test
    void cancelUnknownCartReturnsNotFound() {
        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.PUT("/carts/999999/cancel", null))
        );

        assertEquals(404, exception.getStatus().getCode());
    }

    @Test
    void payCartConfirmsCartAndSetsCartCreatedDate() {
        CartRow cartRow = saveCart(30L, 2, CartStatus.PENDING);
        Date beforePayment = new Date();

        HttpResponse<Cart> response = client.toBlocking().exchange(
                HttpRequest.PUT("/carts/" + cartRow.getId() + "/pay", null),
                Cart.class
        );

        Date afterPayment = new Date();
        assertEquals(200, response.getStatus().getCode());
        assertEquals(CartStatus.CONFIRMED.name(), response.body().status());
        assertNotNull(response.body().cartCreated());
        assertFalse(response.body().cartCreated().before(beforePayment));
        assertFalse(response.body().cartCreated().after(afterPayment));

        CartRow saved = cartRepository.findById(cartRow.getId()).orElseThrow();
        assertEquals(CartStatus.CONFIRMED, saved.getStatus());
        assertNotNull(saved.getCartCreated());
    }

    @Test
    void payUnknownCartReturnsNotFound() {
        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.PUT("/carts/999999/pay", null))
        );

        assertEquals(404, exception.getStatus().getCode());
    }

    @Test
    void payNonPendingCartReturnsConflictAndKeepsHistoryUnchanged() {
        CartRow cartRow = saveCart(31L, 1, CartStatus.CANCELLED);

        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.PUT("/carts/" + cartRow.getId() + "/pay", null)
                )
        );

        assertEquals(409, exception.getStatus().getCode());
        CartRow unchanged = cartRepository.findById(cartRow.getId()).orElseThrow();
        assertEquals(CartStatus.CANCELLED, unchanged.getStatus());
        assertNull(unchanged.getCartCreated());
    }

    @Test
    void productHasOpenCartsReturnsTrueForPendingOrConfirmedCarts() {
        saveCart(20L, 1, CartStatus.PENDING);
        saveCart(20L, 1, CartStatus.CANCELLED);

        HttpResponse<Boolean> response = client.toBlocking().exchange(
                HttpRequest.GET("/carts/product/20/has-open-carts"),
                Boolean.class
        );

        assertEquals(200, response.getStatus().getCode());
        assertTrue(response.body());
    }

    @Test
    void productHasOpenCartsReturnsFalseWithoutPendingOrConfirmedCarts() {
        saveCart(21L, 1, CartStatus.CANCELLED);

        HttpResponse<Boolean> response = client.toBlocking().exchange(
                HttpRequest.GET("/carts/product/21/has-open-carts"),
                Boolean.class
        );

        assertEquals(200, response.getStatus().getCode());
        assertEquals(false, response.body());
    }

    private CartRow saveCart(Long productId, int quantity, CartStatus status) {
        CartRow cartRow = new CartRow();
        cartRow.setQuantity(quantity);
        cartRow.setStatus(status);
        cartRow.setItems(List.of(new CartItemRow(null, cartRow, productId, quantity, BigDecimal.ONE)));
        return cartRepository.save(cartRow);
    }

    private CartItemRow item(CartRow cart, Long productId, int quantity) {
        CartItemRow item = new CartItemRow();
        item.setCart(cart);
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}

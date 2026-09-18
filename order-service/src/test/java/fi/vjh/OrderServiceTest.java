package fi.vjh;

import fi.vjh.domain.Order;
import fi.vjh.domain.OrderStatus;
import fi.vjh.repository.OrderRepository;
import fi.vjh.repository.OrderRow;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@Property(name = "micronaut.server.port", value = "-1")
class OrderServiceTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    OrderRepository orderRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    @Test
    void getAllOrdersReturnsOrders() {
        OrderRow first = saveOrder(1L, 2, OrderStatus.PENDING);
        saveOrder(2L, 1, OrderStatus.CANCELLED);

        List<Order> orders = client.toBlocking().retrieve(
                HttpRequest.GET("/orders"),
                Argument.listOf(Order.class)
        );

        assertEquals(2, orders.size());
        assertTrue(orders.stream().anyMatch(order ->
                first.getId().equals(order.id())
                        && order.productId().equals(1L)
                        && order.quantity().equals(2)
                        && order.status().equals(OrderStatus.PENDING.name())
        ));
    }

    @Test
    void getOrdersForProductReturnsMatchingOrders() {
        OrderRow matching = saveOrder(7L, 2, OrderStatus.PENDING);
        saveOrder(8L, 1, OrderStatus.CONFIRMED);

        List<Order> orders = client.toBlocking().retrieve(
                HttpRequest.GET("/orders/product/7"),
                Argument.listOf(Order.class)
        );

        assertEquals(1, orders.size());
        assertEquals(matching.getId(), orders.get(0).id());
        assertEquals(7L, orders.get(0).productId());
    }

    @Test
    void getOrderByIdReturnsOrderWhenItExists() {
        OrderRow orderRow = saveOrder(3L, 4, OrderStatus.CONFIRMED);

        HttpResponse<Order> response = client.toBlocking().exchange(
                HttpRequest.GET("/orders/" + orderRow.getId()),
                Order.class
        );

        assertEquals(200, response.getStatus().getCode());
        assertEquals(orderRow.getId(), response.body().id());
        assertEquals(3L, response.body().productId());
        assertEquals(4, response.body().quantity());
        assertEquals(OrderStatus.CONFIRMED.name(), response.body().status());
    }

    @Test
    void getUnknownOrderReturnsNotFound() {
        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().retrieve(HttpRequest.GET("/orders/999999"))
        );

        assertEquals(404, exception.getStatus().getCode());
    }

    @Test
    void createOrderForcesPendingStatus() {
        Order request = new Order(null, 11L, 3, "CANCELLED");

        HttpResponse<Order> response = client.toBlocking().exchange(
                HttpRequest.POST("/orders", request),
                Order.class
        );

        assertEquals(201, response.getStatus().getCode());
        Order saved = response.body();
        assertNotNull(saved.id());
        assertEquals(11L, saved.productId());
        assertEquals(3, saved.quantity());
        assertEquals(OrderStatus.PENDING.name(), saved.status());
    }

    @Test
    void cancelOrderChangesStatusToCancelled() {
        OrderRow orderRow = saveOrder(12L, 1, OrderStatus.PENDING);

        HttpResponse<Order> response = client.toBlocking().exchange(
                HttpRequest.PUT("/orders/" + orderRow.getId() + "/cancel", null),
                Order.class
        );

        assertEquals(200, response.getStatus().getCode());
        assertEquals(OrderStatus.CANCELLED.name(), response.body().status());
        assertEquals(
                OrderStatus.CANCELLED,
                orderRepository.findById(orderRow.getId()).orElseThrow().getStatus()
        );
    }

    @Test
    void cancelUnknownOrderReturnsNotFound() {
        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.PUT("/orders/999999/cancel", null))
        );

        assertEquals(404, exception.getStatus().getCode());
    }

    @Test
    void productHasOpenOrdersReturnsTrueForPendingOrConfirmedOrders() {
        saveOrder(20L, 1, OrderStatus.PENDING);
        saveOrder(20L, 1, OrderStatus.CANCELLED);

        HttpResponse<Boolean> response = client.toBlocking().exchange(
                HttpRequest.GET("/orders/product/20/has-open-orders"),
                Boolean.class
        );

        assertEquals(200, response.getStatus().getCode());
        assertTrue(response.body());
    }

    @Test
    void productHasOpenOrdersReturnsFalseWithoutPendingOrConfirmedOrders() {
        saveOrder(21L, 1, OrderStatus.CANCELLED);

        HttpResponse<Boolean> response = client.toBlocking().exchange(
                HttpRequest.GET("/orders/product/21/has-open-orders"),
                Boolean.class
        );

        assertEquals(200, response.getStatus().getCode());
        assertEquals(false, response.body());
    }

    private OrderRow saveOrder(Long productId, int quantity, OrderStatus status) {
        OrderRow orderRow = new OrderRow();
        orderRow.setProductId(productId);
        orderRow.setQuantity(quantity);
        orderRow.setStatus(status);
        return orderRepository.save(orderRow);
    }
}

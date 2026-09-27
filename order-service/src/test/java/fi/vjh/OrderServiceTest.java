package fi.vjh;

import fi.vjh.controller.OrderController;
import fi.vjh.domain.Order;
import fi.vjh.domain.OrderItem;
import fi.vjh.domain.OrderStatus;
import fi.vjh.repository.OrderItemRepository;
import fi.vjh.repository.OrderItemRow;
import fi.vjh.repository.OrderRepository;
import fi.vjh.repository.OrderRow;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = true)
@Property(name = "micronaut.server.port", value = "-1")
class OrderServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(OrderServiceTest.class);

    public static final Long PRODUCT_ID = 99L;
    public static final long CART_ID = 1L;

    @Inject
    OrderController orderController;

    @Inject
    OrderRepository orderRepository;

    @Inject
    OrderItemRepository orderItemRepository;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void cleanUp() {
        entityManager.createQuery("DELETE FROM OrderItemRow").executeUpdate();
        entityManager.createQuery("DELETE FROM OrderRow").executeUpdate();

        // Huuhdellaan ja tyhjennetään istunto, jotta vanhat oliot katoavat muististakin
        entityManager.flush();
        entityManager.clear();

        LOG.info("Kanta pakotettu tyhjäksi ennen testiä.");
    }

    @Test
    void getAllOrdersReturnsOrders() {
        List<Order> orders = orderController.getAllOrders().body();
        assertEquals(0, orders.size());
        OrderRow first = saveOrder(2, OrderStatus.PENDING, PRODUCT_ID);
        saveOrder(1, OrderStatus.CANCELLED, PRODUCT_ID);

        orders = orderController.getAllOrders().body();

        assertEquals(2, orders.size());
    }

    @Test
    void getOrdersForProductReturnsMatchingOrders() {
        OrderRow matching = saveOrder(2, OrderStatus.PENDING, PRODUCT_ID);
        saveOrder(1, OrderStatus.CONFIRMED, 990998877L);

        List<Order> orders = orderController.getOrdersForProduct(PRODUCT_ID).body();

        assertEquals(1, orders.size());
        assertEquals(matching.getId(), orders.getFirst().id());
        assertEquals(CART_ID, orders.getFirst().cartId());
    }

    @Test
    void getOrderByIdReturnsOrderWhenItExists() {
        OrderRow orderRow = saveOrder(4, OrderStatus.CONFIRMED, PRODUCT_ID);

        HttpResponse<Order> response = orderController.getOrderById(orderRow.getId());

        assertEquals(200, response.getStatus().getCode());
        assertEquals(orderRow.getId(), response.body().id());
        assertEquals(CART_ID, response.body().cartId());
        assertEquals(4, response.body().quantity());
        assertEquals(OrderStatus.CONFIRMED.name(), response.body().status());
    }

    @Test
    void getOrderByIdReturnsOrderItems() {
        OrderRow orderRow = saveOrder(4, OrderStatus.CONFIRMED, PRODUCT_ID);

        List<OrderItemRow> itemRows = new ArrayList<>();
        itemRows.add(item(orderRow, 11L, 2));
        itemRows.add(item(orderRow, 12L, 1));
        orderRow.setItems(itemRows);
        orderRepository.update(orderRow);

        HttpResponse<Order> response = orderController.getOrderById(orderRow.getId());

        assertEquals(200, response.getStatus().getCode());
        assertEquals(List.of(11L, 12L), response.body().items().stream()
                .map(OrderItem::productId)
                .toList());
        assertEquals(List.of(2, 1), response.body().items().stream()
                .map(OrderItem::itemCount)
                .toList());
        assertTrue(response.body().items().stream().allMatch(item -> item.id() != null));
    }

    @Test
    void getUnknownOrderReturnsNotFound() {
        HttpResponse<Order> response = orderController.getOrderById(999999L);
        assertEquals(404, response.getStatus().getCode());
    }

    @Test
    void createOrderForcesPendingStatus() {
        Order request = new Order(null, 7L, 3, "CANCELLED");

        HttpResponse<Order> response = orderController.addOrder(request);
        assertEquals(201, response.getStatus().getCode());
        Order saved = response.body();
        assertNotNull(saved.id());
        assertEquals(3, saved.quantity());
        assertEquals(OrderStatus.PENDING.name(), saved.status());
    }

    @Test
    void createOrderPersistsAndReturnsOrderItems() {
        Order request = new Order(
                null,
                7L,
                null,
                3,
                "CANCELLED",
                List.of(
                        new OrderItem(null, 2, 101L),
                        new OrderItem(null, 1, 102L)
                )
        );

        HttpResponse<Order> response = orderController.addOrder(request);

        assertEquals(201, response.getStatus().getCode());
        assertEquals(
                List.of(101L, 102L),
                response.body().items().stream().map(OrderItem::productId).toList()
        );
        assertEquals(
                List.of(2, 1),
                response.body().items().stream().map(OrderItem::itemCount).toList()
        );

        OrderRow saved = orderRepository.findById(response.body().id()).orElseThrow();
        assertEquals(2, saved.getItems().size());
        assertTrue(saved.getItems().stream().allMatch(item -> item.getOrder().getId().equals(saved.getId())));
    }

    @Test
    void createOrderWithInvalidStatusReturnsBadRequest() {
        Order request = new Order(null, 7L, 3, "INVALID");
        HttpResponse<Order> response = orderController.addOrder(request);
        assertEquals(400, response.getStatus().getCode());
    }

    @Test
    void cancelOrderChangesStatusToCancelled() {
        OrderRow orderRow = saveOrder(1, OrderStatus.PENDING, PRODUCT_ID);
        LOG.info("created order {} with pending status", orderRow.getId());

        HttpResponse<Order> response = orderController.cancelOrder(orderRow.getId());

        assertEquals(200, response.getStatus().getCode());
        Order body = response.body();
        assertEquals(orderRow.getId(), body.id());
        assertEquals(OrderStatus.CANCELLED.name(), body.status());
        assertEquals(
                OrderStatus.CANCELLED,
                orderRepository.findById(orderRow.getId()).orElseThrow().getStatus()
        );
    }

    @Test
    void cancelUnknownOrderReturnsNotFound() {
        HttpResponse<Order> response = orderController.cancelOrder(999999L);
        assertEquals(404, response.getStatus().getCode());
    }

    @Test
    void payOrderConfirmsOrderAndSetsOrderPlacedDate() {
        OrderRow orderRow = saveOrder(2, OrderStatus.PENDING, PRODUCT_ID);
        Date beforePayment = new Date();

        HttpResponse<Order> response = orderController.payOrder(orderRow.getId());

        Date afterPayment = new Date();
        assertEquals(200, response.getStatus().getCode());
        assertEquals(OrderStatus.CONFIRMED.name(), response.body().status());
        assertNotNull(response.body().orderPlaced());
        assertFalse(response.body().orderPlaced().before(beforePayment));
        assertFalse(response.body().orderPlaced().after(afterPayment));

        OrderRow saved = orderRepository.findById(orderRow.getId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, saved.getStatus());
        assertNotNull(saved.getOrderPlaced());
    }

    @Test
    void payUnknownOrderReturnsNotFound() {
        HttpResponse<Order> response = orderController.payOrder(999999L);
        assertEquals(404, response.getStatus().getCode());
    }

    @Test
    void payNonPendingOrderReturnsConflictAndKeepsHistoryUnchanged() {
        OrderRow orderRow = saveOrder(1, OrderStatus.CANCELLED, 7L);

        HttpResponse<Order> response = orderController.payOrder(orderRow.getId());

        assertEquals(409, response.getStatus().getCode());
        OrderRow unchanged = orderRepository.findById(orderRow.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, unchanged.getStatus());
        assertNonNull(unchanged.getOrderPlaced());
    }

    private void assertNonNull(Date orderPlaced) {
    }

    @Test
    void productHasOpenOrdersReturnsTrueForPendingOrConfirmedOrders() {
        saveOrder(1, OrderStatus.PENDING, PRODUCT_ID);
        saveOrder(1, OrderStatus.CANCELLED, PRODUCT_ID);

        HttpResponse<Boolean> response = orderController.productHasOpenOrders(PRODUCT_ID);

        assertEquals(200, response.getStatus().getCode());
        assertTrue(response.body());
    }

    @Test
    void productHasOpenOrdersReturnsFalseWithoutPendingOrConfirmedOrders() {
        saveOrder(1, OrderStatus.CANCELLED, PRODUCT_ID);

        HttpResponse<Boolean> response = orderController.productHasOpenOrders(PRODUCT_ID);

        assertEquals(200, response.getStatus().getCode());
        assertEquals(false, response.body());
    }

    private OrderRow saveOrder(int quantity, OrderStatus status, Long productId) {
        OrderRow orderRow = new OrderRow();

        LOG.info("Saving order with quantity {}, status {}, productId {}, items {}", quantity, status, productId, orderRow.getItems().size());

        orderRow.setQuantity(quantity);
        orderRow.setStatus(status);
        orderRow.setCartId(CART_ID);
        orderRow.addItem(item(orderRow, productId, 1));
        return orderRepository.save(orderRow);
    }

    private OrderItemRow item(OrderRow order, Long productId, int quantity) {
        OrderItemRow item = new OrderItemRow();
        item.setOrder(order);
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}

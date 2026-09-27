package fi.vjh.domain;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void fullConstructorPreservesOrderData() {
        Date orderPlaced = new Date();
        OrderItem item = new OrderItem(10L, 2, 99L);
        List<OrderItem> items = List.of(item);

        Order order = new Order(1L, 7L, orderPlaced, 2, "PENDING", items);

        assertEquals(1L, order.id());
        assertEquals(7L, order.cartId());
        assertSame(orderPlaced, order.orderPlaced());
        assertEquals(2, order.quantity());
        assertEquals("PENDING", order.status());
        assertEquals(items, order.items());
    }

    @Test
    void legacyConstructorUsesEmptyItemsAndNoOrderDate() {
        Order order = new Order(1L, 7L, 2, "PENDING");

        assertEquals(1L, order.id());
        assertEquals(7L, order.cartId());
        assertNull(order.orderPlaced());
        assertEquals(2, order.quantity());
        assertEquals("PENDING", order.status());
        assertEquals(List.of(), order.items());
    }
}

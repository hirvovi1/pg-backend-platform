package fi.vjh.domain;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class OrderTest {

    @Test
    void fullConstructorPreservesOrderData() {
        Date orderPlaced = new Date();
        OrderItem item = new OrderItem(10L, 2, 99L);
        List<OrderItem> items = List.of(item);

        Order order = new Order(1L, 99L, orderPlaced, 2, "PENDING", items);

        assertEquals(1L, order.id());
        assertEquals(99L, order.productId());
        assertSame(orderPlaced, order.orderPlaced());
        assertEquals(2, order.quantity());
        assertEquals("PENDING", order.status());
        assertEquals(items, order.items());
    }

    @Test
    void legacyConstructorUsesEmptyItemsAndNoOrderDate() {
        Order order = new Order(1L, 99L, 2, "PENDING");

        assertEquals(1L, order.id());
        assertEquals(99L, order.productId());
        assertEquals(2, order.quantity());
        assertEquals("PENDING", order.status());
        assertNull(order.orderPlaced());
        assertEquals(List.of(), order.items());
    }
}

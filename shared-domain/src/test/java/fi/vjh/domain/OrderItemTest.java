package fi.vjh.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderItemTest {

    @Test
    void exposesItemData() {
        OrderItem item = new OrderItem(10L, 3, 99L);

        assertEquals(10L, item.id());
        assertEquals(3, item.itemCount());
        assertEquals(99L, item.productId());
    }
}

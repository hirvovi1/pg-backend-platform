package fi.vjh.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductTest {

    @Test
    void exposesProductData() {
        Product product = new Product(
                1L,
                "Coffee",
                "Dark roast",
                new BigDecimal("12.50"),
                "https://example.com/coffee",
                ProductStatus.ACTIVE
        );

        assertEquals(1L, product.id());
        assertEquals("Coffee", product.name());
        assertEquals("Dark roast", product.description());
        assertEquals(new BigDecimal("12.50"), product.price());
        assertEquals("https://example.com/coffee", product.imageUrl());
        assertEquals(ProductStatus.ACTIVE, product.status());
    }
}

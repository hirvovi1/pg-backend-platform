package fi.vjh.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductTest {

    @Test
    void exposesProductData() {
        Product product = new Product(
                1L,
                "Coffee",
                "Dark roast",
                1250L,
                "https://example.com/coffee",
                ProductStatus.ACTIVE
        );

        assertEquals(1L, product.id());
        assertEquals("Coffee", product.name());
        assertEquals("Dark roast", product.description());
        assertEquals(1250L, product.priceInCents());
        assertEquals("https://example.com/coffee", product.imageUrl());
        assertEquals(ProductStatus.ACTIVE, product.status());
    }
}

package fi.vjh.service;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;

@Client("${order-service.url}")
public interface OrderServicePort {

    @Get("/orders/product/{productId}/has-open-orders")
    boolean productHasOpenOrders(Long productId);

}

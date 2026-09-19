package fi.vjh.facade;

import fi.vjh.domain.Order;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class DefaultOrderServiceClient implements OrderServiceClient {

    private final HttpClient client;

    public DefaultOrderServiceClient(@Client("http://order-service:8083") HttpClient client) {
        this.client = client;
    }

    @Override
    public List<Order> getOrders() {
        return client.toBlocking().retrieve(HttpRequest.GET("/orders"), Argument.listOf(Order.class));
    }

    @Override
    public List<Order> getOrdersForProduct(Long productId) {
        return client.toBlocking().retrieve(
                HttpRequest.GET("/orders/product/" + productId),
                Argument.listOf(Order.class)
        );
    }

    @Override
    public boolean productHasOpenOrders(Long productId) {
        return client.toBlocking().retrieve(
                HttpRequest.GET("/orders/product/" + productId + "/has-open-orders"),
                Boolean.class
        );
    }

    @Override
    public Order getOrderById(Long id) {
        return client.toBlocking().retrieve(HttpRequest.GET("/orders/" + id), Order.class);
    }

    @Override
    public Order addOrder(Order order) {
        return client.toBlocking().retrieve(HttpRequest.POST("/orders", order), Order.class);
    }

    @Override
    public Order cancelOrder(Long id) {
        return client.toBlocking().retrieve(HttpRequest.PUT("/orders/" + id + "/cancel", ""), Order.class);
    }

    @Override
    public Order payOrder(Long id) {
        return client.toBlocking().retrieve(HttpRequest.PUT("/orders/" + id + "/pay", ""), Order.class);
    }
}

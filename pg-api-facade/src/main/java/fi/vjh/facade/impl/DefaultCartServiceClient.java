package fi.vjh.facade.impl;

import fi.vjh.domain.Cart;
import fi.vjh.facade.CartServiceClient;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class DefaultCartServiceClient implements CartServiceClient {

    private final HttpClient client;

    public DefaultCartServiceClient(@Client("http://cart-service:8084") HttpClient client) {
        this.client = client;
    }

    @Override
    public List<Cart> getCarts() {
        return client.toBlocking().retrieve(HttpRequest.GET("/carts"), Argument.listOf(Cart.class));
    }

    @Override
    public List<Cart> getCartsForProduct(Long productId) {
        return client.toBlocking().retrieve(
                HttpRequest.GET("/carts/product/" + productId),
                Argument.listOf(Cart.class)
        );
    }

    @Override
    public boolean productHasOpenCarts(Long productId) {
        return client.toBlocking().retrieve(
                HttpRequest.GET("/carts/product/" + productId + "/has-open-carts"),
                Boolean.class
        );
    }

    @Override
    public Cart getCartById(Long id) {
        return client.toBlocking().retrieve(HttpRequest.GET("/carts/" + id), Cart.class);
    }

    @Override
    public Cart addCart(Cart cart) {
        return client.toBlocking().retrieve(HttpRequest.POST("/carts", cart), Cart.class);
    }

    @Override
    public Cart cancelCart(Long id) {
        return client.toBlocking().retrieve(HttpRequest.PUT("/carts/" + id + "/cancel", ""), Cart.class);
    }

    @Override
    public Cart payCart(Long id) {
        return client.toBlocking().retrieve(HttpRequest.PUT("/carts/" + id + "/pay", ""), Cart.class);
    }
}

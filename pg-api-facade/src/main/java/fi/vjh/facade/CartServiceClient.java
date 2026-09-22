package fi.vjh.facade;

import fi.vjh.domain.Cart;

import java.util.List;

public interface CartServiceClient {

    List<Cart> getCarts();

    List<Cart> getCartsForProduct(Long productId);

    boolean productHasOpenCarts(Long productId);

    Cart getCartById(Long id);

    Cart addCart(Cart cart);

    Cart cancelCart(Long id);

    Cart payCart(Long id);
}

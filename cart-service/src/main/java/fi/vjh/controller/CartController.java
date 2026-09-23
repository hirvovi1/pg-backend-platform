package fi.vjh.controller;

import fi.vjh.domain.Cart;
import fi.vjh.domain.CartItem;
import fi.vjh.domain.CartStatus;
import fi.vjh.repository.CartItemRow;
import fi.vjh.repository.CartRepository;
import fi.vjh.repository.CartRow;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.runtime.event.annotation.EventListener;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller("/carts")
@RequiredArgsConstructor
public class CartController {

    private static final Logger LOG = LoggerFactory.getLogger(CartController.class);

    private final CartRepository cartRepository;
    @Value("${app.version}")
    private String version;

    @EventListener
    public void onEvent(StartupEvent event) {
        LOG.info("Cart service version {}", version);
    }

    @Get
    public List<Cart> getAllCarts() {
        return cartRepository.findAll().stream()
                .map(this::convertToCart)
                .toList();
    }

    @Get("/product/{productId}")
    public List<Cart> getCartsForProduct(Long productId) {
        return cartRepository.findByItemsProductId(productId).stream()
                .map(this::convertToCart)
                .toList();
    }

    @Post("/create")
    public HttpResponse<Cart> createCart() {
        CartRow cart = cartRepository.save(new CartRow());
        return HttpResponse.created(convertToCart(cart));
    }

    @Get("/{id}")
    public HttpResponse<Cart> getCartById(Long id) {
        return cartRepository.findById(id)
                .map(this::convertToCart)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Post
    public HttpResponse<Cart> saveCart(@Body Cart cart) {
        final CartRow cartRow;
        try {
            cartRow = convertToCartRow(cart);
        } catch (IllegalArgumentException exception) {
            return HttpResponse.badRequest();
        }
        cartRow.setStatus(CartStatus.PENDING);
        return HttpResponse.created(convertToCart(cartRepository.save(cartRow)));
    }

    @Put("/{id}/cancel")
    public HttpResponse<Cart> cancelCart(Long id) {
        return cartRepository.findById(id)
                .map(cartRow -> {
                    cartRow.setStatus(CartStatus.CANCELLED);
                    return HttpResponse.ok(convertToCart(cartRepository.update(cartRow)));
                })
                .orElse(HttpResponse.notFound());
    }

    @Put("/{id}/pay")
    public HttpResponse<Cart> payCart(Long id) {
        return cartRepository.findById(id)
                .map(cartRow -> {
                    if (cartRow.getStatus() != CartStatus.PENDING) {
                        return HttpResponse.<Cart>status(HttpStatus.CONFLICT);
                    }
                    cartRow.setStatus(CartStatus.CONFIRMED);
                    cartRow.setCartCreated(new Date());
                    return HttpResponse.ok(convertToCart(cartRepository.update(cartRow)));
                })
                .orElse(HttpResponse.notFound());
    }

    @Get("/product/{productId}/has-open-carts")
    public HttpResponse<Boolean> productHasOpenCarts(Long productId) {
        boolean hasOpenCarts = !cartRepository.findByItemsProductIdAndStatusIn(
                productId,
                List.of(CartStatus.PENDING, CartStatus.CONFIRMED)
        ).isEmpty();
        return HttpResponse.status(HttpStatus.OK).body(hasOpenCarts);
    }

    private Cart convertToCart(CartRow row) {
        return new Cart(
                row.getId(),
                row.getCartCreated(),
                row.getStatus().name(),
                mapItems(row.getItems())
        );
    }

    private CartRow convertToCartRow(Cart cart) {
        CartRow row = new CartRow();
        row.setId(cart.id());
        row.setCartCreated(cart.cartCreated());
        row.setStatus(CartStatus.valueOf(cart.status()));
        row.setItems(mapItemRows(cart.items(), row));
        return row;
    }

    private List<CartItem> mapItems(List<CartItemRow> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new CartItem(item.getId(), item.getQuantity(), item.getProductId(), item.getPriceAtPurchaseInCents()))
                .toList();
    }

    private List<CartItemRow> mapItemRows(List<CartItem> items, CartRow cart) {
        if (items == null) {
            return new ArrayList<>();
        }
        return items.stream()
                .map(item -> {
                    CartItemRow row = new CartItemRow();
                    row.setId(item.id());
                    row.setProductId(item.productId());
                    row.setQuantity(item.itemCount());
                    row.setPriceAtPurchaseInCents(item.priceInCents());
                    row.setCart(cart);
                    return row;
                })
                .toList();
    }
}

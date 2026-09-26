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
import java.util.LinkedList;
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
        LOG.info("++Cart service version {} started successfully", version);
    }

    @Get
    public HttpResponse<List<Cart>> getAllCarts() {
        LOG.debug("Received request to fetch all carts");
        try {
            List<Cart> carts = cartRepository.findAll().stream()
                    .map(this::convertToCart)
                    .toList();
            LOG.info("Successfully fetched {} carts", carts.size());
            return HttpResponse.ok(carts);
        } catch (Exception e) {
            LOG.error("Failed to fetch all carts due to an unexpected error", e);
            return HttpResponse.serverError();
        }
    }

    @Get("/product/{productId}")
    public HttpResponse<List<Cart>> getCartsForProduct(Long productId) {
        LOG.info("Received request to fetch carts containing productId: {}", productId);
        try {
            List<Cart> carts = cartRepository.findByItemsProductId(productId).stream()
                    .map(this::convertToCart)
                    .toList();
            LOG.info("Found {} carts for productId: {}", carts.size(), productId);
            return HttpResponse.ok(carts);
        } catch (Exception e) {
            LOG.error("Failed to fetch carts for productId: " + productId, e);
            return HttpResponse.serverError();
        }
    }

    @Post("/create")
    public HttpResponse<Cart> createCart() {
        LOG.info("Received request to create a fresh, empty cart");
        try {
            final CartRow emptyCart = new CartRow();
            emptyCart.setItems(new LinkedList<>());

            CartRow cart = cartRepository.save(emptyCart);
            LOG.info("Successfully created new cart in database with generated ID: {}", cart.getId());
            return HttpResponse.created(convertToCart(cart));
        } catch (Exception e) {
            LOG.error("Failed to create a new cart row", e);
            return HttpResponse.serverError();
        }
    }

    @Get("/{id}")
    public HttpResponse<Cart> getCartById(Long id) {
        LOG.debug("Received request to fetch cart by ID: {}", id);
        try {
            return cartRepository.findById(id)
                    .map(row -> {
                        LOG.info("Cart found for ID: {}", id);
                        return HttpResponse.ok(this.convertToCart(row));
                    })
                    .orElseGet(() -> {
                        LOG.warn("Cart not found for ID: {}", id);
                        return HttpResponse.notFound();
                    });
        } catch (Exception e) {
            LOG.error("Failed to fetch cart by ID: " + id, e);
            return HttpResponse.serverError();
        }
    }

    @Post
    public HttpResponse<Cart> saveCart(@Body Cart cart) {
        LOG.info("Received request to upsert/save cart. Input payload data: {}", cart);
        try {
            final CartRow cartRow = convertToCartRow(cart);
            cartRow.setStatus(CartStatus.PENDING);

            CartRow savedRow = cartRepository.update(cartRow);
            LOG.info("Successfully saved cart. Database Row ID: {}, status reset to PENDING", savedRow.getId());
            return HttpResponse.created(convertToCart(savedRow));
        } catch (IllegalArgumentException e) {
            LOG.error("Failed to map cart DTO to entity due to invalid cart status enum value: {}", cart.status(), e);
            return HttpResponse.badRequest();
        } catch (Exception e) {
            LOG.error("Unexpected error occurred while persisting cart state", e);
            return HttpResponse.serverError();
        }
    }

    @Put("/{id}/cancel")
    public HttpResponse<Cart> cancelCart(Long id) {
        LOG.info("Received request to cancel cart with ID: {}", id);
        try {
            return cartRepository.findById(id)
                    .map(cartRow -> {
                        cartRow.setStatus(CartStatus.CANCELLED);
                        CartRow updated = cartRepository.update(cartRow);
                        LOG.info("Cart with ID: {} successfully updated to CANCELLED", id);
                        return HttpResponse.ok(convertToCart(updated));
                    })
                    .orElseGet(() -> {
                        LOG.warn("Cannot cancel cart: ID {} not found", id);
                        return HttpResponse.notFound();
                    });
        } catch (Exception e) {
            LOG.error("Failed to execute cancel operation on cart ID: " + id, e);
            return HttpResponse.serverError();
        }
    }

    @Put("/{id}/pay")
    public HttpResponse<Cart> payCart(Long id) {
        LOG.info("Received execution request to mark cart ID: {} as PAID/CONFIRMED", id);
        try {
            return cartRepository.findById(id)
                    .map(cartRow -> {
                        if (cartRow.getStatus() != CartStatus.PENDING) {
                            LOG.warn("Payment conflict: Cart ID {} is in state [{}] but must be PENDING to proceed", id, cartRow.getStatus());
                            return HttpResponse.<Cart>status(HttpStatus.CONFLICT);
                        }
                        cartRow.setStatus(CartStatus.CONFIRMED);
                        cartRow.setCartCreated(new Date());

                        CartRow updated = cartRepository.update(cartRow);
                        LOG.info("Payment registered successfully. Cart ID: {} status updated to CONFIRMED", id);
                        return HttpResponse.ok(convertToCart(updated));
                    })
                    .orElseGet(() -> {
                        LOG.warn("Cannot pay cart: ID {} not found", id);
                        return HttpResponse.notFound();
                    });
        } catch (Exception e) {
            LOG.error("Unexpected critical error occurred during the payment mapping of cart ID: " + id, e);
            return HttpResponse.serverError();
        }
    }

    @Get("/product/{productId}/has-open-carts")
    public HttpResponse<Boolean> productHasOpenCarts(Long productId) {
        LOG.debug("Checking open active carts (PENDING/CONFIRMED) for productId: {}", productId);
        try {
            boolean hasOpenCarts = !cartRepository.findByItemsProductIdAndStatusIn(
                    productId,
                    List.of(CartStatus.PENDING, CartStatus.CONFIRMED)
            ).isEmpty();
            LOG.info("Open carts check for product {}: result = {}", productId, hasOpenCarts);
            return HttpResponse.status(HttpStatus.OK).body(hasOpenCarts);
        } catch (Exception e) {
            LOG.error("Failed to query database for open active carts regarding productId: " + productId, e);
            return HttpResponse.serverError();
        }
    }

    private Cart convertToCart(CartRow row) {
        return new Cart(
                row.getId(),
                row.getCartCreated(),
                row.getStatus() != null ? row.getStatus().name() : null,
                mapItems(row.getItems())
        );
    }

    private CartRow convertToCartRow(Cart cart) {
        CartRow row = new CartRow();
        row.setId(cart.id());
        row.setCartCreated(cart.cartCreated());
        if (cart.status() != null) {
            row.setStatus(CartStatus.valueOf(cart.status()));
        }
        row.setItems(mapItemRows(cart.items(), row));
        return row;
    }

    private List<CartItem> mapItems(List<CartItemRow> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item ->
                        new CartItem(item.getId(), item.getQuantity(), item.getProductId(), item.getProductName(),
                                item.getPriceAtPurchaseInCents()))
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
                    row.setProductName(item.productName());
                    row.setQuantity(item.itemCount());
                    row.setPriceAtPurchaseInCents(item.priceInCents());
                    row.setCart(cart);
                    return row;
                })
                .toList();
    }
}

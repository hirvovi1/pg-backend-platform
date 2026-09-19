package fi.vjh.facade;

import fi.vjh.domain.Order;
import fi.vjh.domain.Product;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import io.micronaut.http.sse.Event;
import java.util.List;
import java.util.Map;

/**
 * Frontend-facing facade that delegates requests to the backend services.
 */
@Controller("/api/v1/frontend")
@Serdeable
@RequiredArgsConstructor
@ExecuteOn(TaskExecutors.BLOCKING)
public class FacadeController {

    private final OrderServiceClient orderService;
    private final ProductServiceClient productService;
    private final CurrencyServiceClient currencyService;
    private final HealthPulseServiceClient healthPulseService;

    /**
     * Returns all active products.
     *
     * @return products delegated from the product service
     */
    @Get("/products")
    public List<Product> getProducts() {
        return productService.getProducts();
    }

    /**
     * Returns a product by its identifier.
     *
     * @param id product identifier
     * @return product delegated from the product service
     */
    @Get("/products/{id}")
    public Product getProductById(Long id) {
        return productService.getProductById(id);
    }

    /**
     * Creates a product through the product service.
     *
     * @param product product to create
     * @return created product delegated from the product service
     */
    @Post("/products")
    public Product addProduct(@Body Product product) {
        return productService.addProduct(product);
    }

    /**
     * Archives a product through the product service.
     *
     * @param id product identifier
     * @return no content when the product is archived successfully
     */
    @Delete("/products/{id}")
    public void deleteProduct(Long id) {
        productService.deleteProduct(id);
    }

    /**
     * Returns all orders.
     *
     * @return orders delegated from the order service
     */
    @Get("/orders")
    public List<Order> getOrders() {
        return orderService.getOrders();
    }

    /**
     * Returns orders associated with a product.
     *
     * @param productId product identifier
     * @return matching orders delegated from the order service
     */
    @Get("/orders/product/{productId}")
    public List<Order> getOrdersForProduct(Long productId) {
        return orderService.getOrdersForProduct(productId);
    }

    /**
     * Checks whether a product has pending or confirmed orders.
     *
     * @param productId product identifier
     * @return {@code true} when the product has open orders
     */
    @Get("/orders/product/{productId}/has-open-orders")
    public boolean productHasOpenOrders(Long productId) {
        return orderService.productHasOpenOrders(productId);
    }

    /**
     * Returns an order by its identifier.
     *
     * @param id order identifier
     * @return order delegated from the order service
     */
    @Get("/orders/{id}")
    public Order getOrderById(Long id) {
        return orderService.getOrderById(id);
    }

    /**
     * Creates an order through the order service.
     *
     * @param order order to create
     * @return created order delegated from the order service
     */
    @Post("/orders")
    public Order addOrder(@Body Order order) {
        return orderService.addOrder(order);
    }

    /**
     * Cancels an order through the order service.
     *
     * @param id order identifier
     * @return cancelled order delegated from the order service
     */
    @Put("/orders/{id}/cancel")
    public Order cancelOrder(Long id) {
        return orderService.cancelOrder(id);
    }

    /**
     * Pays for an order through the order service.
     *
     * @param id order identifier
     * @return paid order delegated from the order service
     */
    @Put("/orders/{id}/pay")
    public Order payOrder(Long id) {
        return orderService.payOrder(id);
    }

    /**
     * Converts an amount from cents to USD.
     *
     * @param amountInCents amount to convert in cents
     * @return currency conversion data delegated from the currency service
     */
    @Get("/currency/convert")
    public Map<String, Object> convertToUsd(@QueryValue double amountInCents) {
        return currencyService.convertToUsd(amountInCents);
    }

    /**
     * Streams health updates from the health pulse service.
     *
     * @return delegated server-sent health events
     */
    @Get(value = "/health/stream", produces = MediaType.TEXT_EVENT_STREAM)
    public Publisher<Event<Map<String, Object>>> getHealthStream() {
        return healthPulseService.getHealthStream();
    }
}

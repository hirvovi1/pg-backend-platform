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

    @Get("/products")
    public List<Product> getProducts() {
        return productService.getProducts();
    }

    @Get("/products/{id}")
    public Product getProductById(Long id) {
        return productService.getProductById(id);
    }

    @Post("/products")
    public Product addProduct(@Body Product product) {
        return productService.addProduct(product);
    }

    @Delete("/products/{id}")
    public void deleteProduct(Long id) {
        productService.deleteProduct(id);
    }

    @Get("/orders")
    public List<Order> getOrders() {
        return orderService.getOrders();
    }

    @Get("/orders/product/{productId}")
    public List<Order> getOrdersForProduct(Long productId) {
        return orderService.getOrdersForProduct(productId);
    }

    @Get("/orders/product/{productId}/has-open-orders")
    public boolean productHasOpenOrders(Long productId) {
        return orderService.productHasOpenOrders(productId);
    }

    @Get("/orders/{id}")
    public Order getOrderById(Long id) {
        return orderService.getOrderById(id);
    }

    @Post("/orders")
    public Order addOrder(@Body Order order) {
        return orderService.addOrder(order);
    }

    @Put("/orders/{id}/cancel")
    public Order cancelOrder(Long id) {
        return orderService.cancelOrder(id);
    }

    @Put("/orders/{id}/pay")
    public Order payOrder(Long id) {
        return orderService.payOrder(id);
    }

    @Get("/currency/convert")
    public Map<String, Object> convertToUsd(@QueryValue double amountInCents) {
        return currencyService.convertToUsd(amountInCents);
    }

    @Get(value = "/health/stream", produces = MediaType.TEXT_EVENT_STREAM)
    public Publisher<Event<Map<String, Object>>> getHealthStream() {
        return healthPulseService.getHealthStream();
    }
}

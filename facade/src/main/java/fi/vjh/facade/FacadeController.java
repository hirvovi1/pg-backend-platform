package fi.vjh.facade;

import fi.vjh.domain.Order;
import fi.vjh.domain.Product;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Frontend-facing facade that delegates requests to the backend services.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "PG API platform",
                version = "${api.version}",
                description = "Julkinen rajapintakerros (Facade)"
        )
)
@Controller("/api/v1/frontend")
@Serdeable
@RequiredArgsConstructor
@ExecuteOn(TaskExecutors.BLOCKING)
public class FacadeController {

    private static final Logger LOG = LoggerFactory.getLogger(FacadeController.class);

    private final OrderServiceClient orderService;
    private final ProductServiceClient productService;
    private final CurrencyServiceClient currencyService;
    @Value("${app.version}")
    private String version;

    @EventListener
    public void onEvent(StartupEvent event) {
        LOG.info("Facade api version {}", version);
    }

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
}

package fi.vjh.controller;

import fi.vjh.domain.Order;
import fi.vjh.domain.OrderItem;
import fi.vjh.domain.OrderStatus;
import fi.vjh.repository.OrderItemRow;
import fi.vjh.repository.OrderRepository;
import fi.vjh.repository.OrderRow;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.annotation.NonNull;
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
import java.util.Optional;

@Controller("/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final Logger LOG = LoggerFactory.getLogger(OrderController.class);

    private final OrderRepository orderRepository;
    @Value("${app.version}")
    private String version;

    @EventListener
    public void onEvent(StartupEvent event) {
        LOG.info("Order service version {}", version);
    }

    @Get
    public HttpResponse<List<Order>> getAllOrders() {
        List<Order> orders = orderRepository.findAll().stream()
                .map(this::convertToOrder)
                .toList();
        return HttpResponse.ok(orders);
    }

    @Get("/product/{productId}")
    public HttpResponse<List<Order>> getOrdersForProduct(Long productId) {
        List<Order> orders = orderRepository
                .findByItemsProductIdAndStatusIn(productId, List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED))
                .stream().map(this::convertToOrder).toList();
        debugOrders(orders);
        LOG.info("Found {} orders for product {}", orders.size(), productId);
        LOG.info("listing all orders from db");
        debugOrderRows(orderRepository.findAll());
        LOG.info("--> end. returning status ok **************************************");
        return HttpResponse.ok(orders);
    }

    private void debugOrderRows(@NonNull List<OrderRow> all) {
        LOG.info("Found {} orders in db", all.size());
        for (OrderRow orderRow : all) {
            LOG.info("Order {} has status {}", orderRow.getId(), orderRow.getStatus());

            LOG.info("listing order items:");
            for (OrderItemRow item : orderRow.getItems()) {
                LOG.info("Order item {} with product id {} has quantity {}", item.getId(), item.getProductId(), item.getQuantity());
            }
        }
    }

    private void debugOrders(List<Order> orders) {
        LOG.info("Found {} orders for product", orders.size());
        for (Order order : orders) {
            LOG.info("Order {} has status {}", order.id(), order.status());

            LOG.info("listing order items:");
            for (OrderItem item : order.items()) {
                LOG.info("Order item {} with product id {} has quantity {}", item.id(), item.productId(), item.itemCount());
            }
        }
    }

    @Get("/product/{productId}/has-open-orders")
    public HttpResponse<Boolean> productHasOpenOrders(Long productId) {
        LOG.info("Checking for open orders for product {}", productId);
        List<OrderRow> orders = orderRepository.findByItemsProductIdAndStatusIn(
                productId,
                List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED)
        );
        debugOrderRows(orders);
        boolean hasOpenOrders = !orders.isEmpty();
        return HttpResponse.status(HttpStatus.OK).body(hasOpenOrders);
    }

    @Get("/{id}")
    public HttpResponse<Order> getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(this::convertToOrder)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Post
    public HttpResponse<Order> addOrder(@Body Order order) {
        final OrderRow orderRow;
        try {
            orderRow = convertToOrderRow(order);
        } catch (IllegalArgumentException exception) {
            return HttpResponse.badRequest();
        }
        orderRow.setStatus(OrderStatus.PENDING);
        return HttpResponse.created(convertToOrder(orderRepository.save(orderRow)));
    }

    @Put("/{id}/cancel")
    public HttpResponse<Order> cancelOrder(Long id) {
        Optional<OrderRow> order = orderRepository.findById(id);
        if (order.isPresent()) {
            LOG.info("Canceling order {}", id);
            OrderRow orderRow = order.get();
            orderRow.setStatus(OrderStatus.CANCELLED);
            Order converted = convertToOrder(orderRepository.update(orderRow));
            return HttpResponse.ok(converted);
        }
        return HttpResponse.notFound();
    }

    @Put("/{id}/pay")
    public HttpResponse<Order> payOrder(Long id) {
        LOG.info("Paying order {}", id);
        @NonNull Optional<OrderRow> optional = orderRepository.findById(id);
        if (optional.isEmpty()) return HttpResponse.notFound();
        OrderRow orderRow = optional.get();
        LOG.info("Found order {}", orderRow);

        if (orderRow.getStatus() == OrderStatus.CONFIRMED) {
            LOG.info("Order {} is already in CONFIRMED status. skipping.", id);
            return HttpResponse.ok(convertToOrder(orderRow));
        } else if (orderRow.getStatus() == OrderStatus.CANCELLED) {
            LOG.info("Order {} is not in PENDING status. Current status: {}. Cannot pay.", id, orderRow.getStatus());
            return HttpResponse.<Order>status(HttpStatus.CONFLICT);
        } else if (orderRow.getStatus() == OrderStatus.PENDING) {
            LOG.info("Order {} is in PENDING status. updating to CONFIRMED.", id);
            orderRow.setStatus(OrderStatus.CONFIRMED);
            orderRow.setOrderPlaced(new Date());
            return HttpResponse.ok(convertToOrder(orderRepository.update(orderRow)));
        }
        throw new IllegalArgumentException("invalid status: " + orderRow.getStatus());
    }

    private Order convertToOrder(OrderRow row) {
        return new Order(
                row.getId(),
                row.getCartId(),
                row.getOrderPlaced(),
                row.getQuantity(),
                row.getStatus().name(),
                mapItems(row.getItems())
        );
    }

    private OrderRow convertToOrderRow(Order order) {
        OrderRow row = new OrderRow();
        row.setId(order.id());
        row.setCartId(order.cartId());
        row.setQuantity(order.quantity());
        row.setOrderPlaced(order.orderPlaced());
        row.setStatus(OrderStatus.valueOf(order.status()));
        row.setItems(mapItemRows(order.items(), row));
        return row;
    }

    private List<OrderItem> mapItems(List<OrderItemRow> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new OrderItem(item.getId(), item.getQuantity(), item.getProductId()))
                .toList();
    }

    private List<OrderItemRow> mapItemRows(List<OrderItem> items, OrderRow order) {
        if (items == null) {
            return new ArrayList<>();
        }
        return items.stream()
                .map(item -> {
                    OrderItemRow row = new OrderItemRow();
                    row.setId(item.id());
                    row.setProductId(item.productId());
                    row.setQuantity(item.itemCount());
                    row.setOrder(order);
                    return row;
                })
                .toList();
    }
}

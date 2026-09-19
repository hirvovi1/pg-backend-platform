package fi.vjh.controller;

import fi.vjh.domain.Order;
import fi.vjh.domain.OrderItem;
import fi.vjh.domain.OrderStatus;
import fi.vjh.repository.OrderItemRow;
import fi.vjh.repository.OrderRepository;
import fi.vjh.repository.OrderRow;
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
    public List<Order> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToOrder)
                .toList();
    }

    @Get("/product/{productId}")
    public List<Order> getOrdersForProduct(Long productId) {
        return orderRepository.findByProductId(productId).stream()
                .map(this::convertToOrder)
                .toList();
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
        return orderRepository.findById(id)
                .map(orderRow -> {
                    orderRow.setStatus(OrderStatus.CANCELLED);
                    return HttpResponse.ok(convertToOrder(orderRepository.update(orderRow)));
                })
                .orElse(HttpResponse.notFound());
    }

    @Put("/{id}/pay")
    public HttpResponse<Order> payOrder(Long id) {
        return orderRepository.findById(id)
                .map(orderRow -> {
                    if (orderRow.getStatus() != OrderStatus.PENDING) {
                        return HttpResponse.<Order>status(HttpStatus.CONFLICT);
                    }
                    orderRow.setStatus(OrderStatus.CONFIRMED);
                    orderRow.setOrderPlaced(new Date());
                    return HttpResponse.ok(convertToOrder(orderRepository.update(orderRow)));
                })
                .orElse(HttpResponse.notFound());
    }

    @Get("/product/{productId}/has-open-orders")
    public HttpResponse<Boolean> productHasOpenOrders(Long productId) {
        boolean hasOpenOrders = !orderRepository.findByProductIdAndStatusIn(
                productId,
                List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED)
        ).isEmpty();
        return HttpResponse.status(HttpStatus.OK).body(hasOpenOrders);
    }

    private Order convertToOrder(OrderRow row) {
        return new Order(
                row.getId(),
                row.getProductId(),
                row.getOrderPlaced(),
                row.getQuantity(),
                row.getStatus().name(),
                mapItems(row.getItems())
        );
    }

    private OrderRow convertToOrderRow(Order order) {
        OrderRow row = new OrderRow();
        row.setId(order.id());
        row.setProductId(order.productId());
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

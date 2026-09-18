package fi.vjh.controller;

import fi.vjh.domain.Order;
import fi.vjh.domain.OrderStatus;
import fi.vjh.repository.OrderRepository;
import fi.vjh.repository.OrderRow;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;

import java.util.List;

@Controller("/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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
        OrderRow orderRow = convertToOrderRow(order);
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
                row.getQuantity(),
                row.getStatus().name()
        );
    }

    private OrderRow convertToOrderRow(Order order) {
        OrderRow row = new OrderRow();
        row.setId(order.id());
        row.setProductId(order.productId());
        row.setQuantity(order.quantity());
        row.setStatus(OrderStatus.valueOf(order.status()));
        return row;
    }
}

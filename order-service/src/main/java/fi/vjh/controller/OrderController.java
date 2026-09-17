package fi.vjh.controller;

import fi.vjh.domain.Order;
import fi.vjh.domain.OrderStatus;
import fi.vjh.repository.OrderRepository;
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
        return orderRepository.findAll();
    }

    @Get("/product/{productId}")
    public List<Order> getOrdersForProduct(Long productId) {
        return orderRepository.findByProductId(productId);
    }

    @Get("/{id}")
    public HttpResponse<Order> getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Post
    public HttpResponse<Order> addOrder(@Body Order order) {
        order.setStatus(OrderStatus.PENDING);
        return HttpResponse.created(orderRepository.save(order));
    }

    @Put("/{id}/cancel")
    public HttpResponse<Order> cancelOrder(Long id) {
        return orderRepository.findById(id)
                .map(order -> {
                    order.setStatus(OrderStatus.CANCELLED);
                    return HttpResponse.ok(orderRepository.update(order));
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
}

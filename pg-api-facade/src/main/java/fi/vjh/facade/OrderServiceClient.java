package fi.vjh.facade;

import fi.vjh.domain.Order;

import java.util.List;

public interface OrderServiceClient {

    List<Order> getOrders();

    List<Order> getOrdersForProduct(Long productId);

    boolean productHasOpenOrders(Long productId);

    Order getOrderById(Long id);

    Order addOrder(Order order);

    Order cancelOrder(Long id);

    Order payOrder(Long id);
}

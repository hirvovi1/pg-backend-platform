package fi.vjh;

import fi.vjh.domain.Order;
import fi.vjh.domain.OrderStatus;
import fi.vjh.repository.OrderRepository;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

@Singleton
public class DataInitializer {

    private final OrderRepository orderRepository;

    public DataInitializer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @EventListener
    @Transactional
    public void onStartup(StartupEvent event) {
        if (orderRepository.count() == 0) {
            Order order = new Order();
            order.setProductId(1L);
            order.setQuantity(1);
            order.setStatus(OrderStatus.PENDING);
            orderRepository.save(order);
        }
    }
}

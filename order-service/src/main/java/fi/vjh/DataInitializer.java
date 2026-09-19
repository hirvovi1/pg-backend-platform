package fi.vjh;

import fi.vjh.repository.OrderRow;
import fi.vjh.domain.OrderStatus;
import fi.vjh.repository.OrderRepository;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class DataInitializer {

    private final OrderRepository orderRepository;

    @EventListener
    @Transactional
    public void onStartup(StartupEvent event) {
        if (orderRepository.count() == 0) {
            OrderRow orderRow = new OrderRow();
            orderRow.setProductId(1L);
            orderRow.setQuantity(1);
            orderRow.setStatus(OrderStatus.PENDING);
            orderRepository.save(orderRow);
        }
    }
}

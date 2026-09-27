package fi.vjh;

import fi.vjh.domain.OrderStatus;
import fi.vjh.repository.OrderItemRow;
import fi.vjh.repository.OrderRepository;
import fi.vjh.repository.OrderRow;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Date;
import java.util.List;

@Singleton
@RequiredArgsConstructor
public class DataInitializer {

    private final OrderRepository orderRepository;

    @EventListener
    @Transactional
    public void onStartup(StartupEvent event) {
        if (orderRepository.count() == 0) {
            OrderRow orderRow = new OrderRow();
            orderRow.setItems(List.of(new OrderItemRow(null, orderRow, 20L, 1, 10001L)));
            orderRow.setQuantity(1);
            orderRow.setStatus(OrderStatus.PENDING);
            orderRepository.save(orderRow);
        }
    }
}

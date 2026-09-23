package fi.vjh;

import fi.vjh.domain.CartStatus;
import fi.vjh.repository.CartRepository;
import fi.vjh.repository.CartRow;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class DataInitializer {

    private final CartRepository cartRepository;

    @EventListener
    @Transactional
    public void onStartup(StartupEvent event) {
        if (cartRepository.count() == 0) {
            CartRow cartRow = new CartRow();
            cartRow.setQuantity(1);
            cartRow.setStatus(CartStatus.PENDING);
            cartRepository.save(cartRow);
        }
    }
}

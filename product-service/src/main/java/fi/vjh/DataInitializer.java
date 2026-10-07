package fi.vjh;

import fi.vjh.domain.ProductStatus;
import fi.vjh.repository.ProductRepository;
import fi.vjh.repository.ProductRow;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class DataInitializer {

    private final ProductRepository productRepository;

    @EventListener
    @Transactional
    public void onStartup(StartupEvent event) {
        if (productRepository.count() == 0) {
            ProductRow p1 = new ProductRow();
            p1.setName("Koodauskahvi");
            p1.setDescription("Tumma paahto, pitää bugit loitolla.");
            p1.setPriceInCents(1250L);
            p1.setImageUrl("https://example.com");
            p1.setStatus(ProductStatus.ACTIVE);
            productRepository.save(p1);

            ProductRow p2 = new ProductRow();
            p2.setName("Micronaut t-paita");
            p2.setDescription("Nopeampi käynnistymisaika kuin puuvillalla yleensä.");
            p2.setPriceInCents(2500L);
            p2.setImageUrl("https://example.com");
            p2.setStatus(ProductStatus.ACTIVE);
            productRepository.save(p2);
        }
    }
}

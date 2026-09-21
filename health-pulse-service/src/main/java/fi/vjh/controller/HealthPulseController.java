package fi.vjh.controller;

import fi.vjh.domain.EndpointURL;
import fi.vjh.util.HealthCheckUtil;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.sse.Event;
import io.micronaut.runtime.event.annotation.EventListener;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 8095
 */
@Controller("/api/pulse")
public class HealthPulseController {

    private static final Logger LOG = LoggerFactory.getLogger(HealthPulseController.class);

    @Value("${app.version}")
    private String version;

    private final Map<String, EndpointURL> endpoints = Map.of(
            "Java account api", EndpointURL.of("http://pgapi:8080/accounts"),
            "currency service", EndpointURL.of("http://usd-currency-service:8090/api/usd/convert?amountInCents=100"),
            "product service", EndpointURL.of("http://product-service:8082/products"),
            "order service", EndpointURL.of("http://order-service:8083/orders"),
            "H2 db (via api)", EndpointURL.of("http://pgapi:8080/db/health")
    );

    @EventListener
    public void onEvent(StartupEvent event) {
        LOG.info("Health pulse service version {}", version);
    }

    @Get(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM)
    public Publisher<Event<Map<String, Object>>> getHealthStream() {
        List<String> serviceNames = new ArrayList<>(endpoints.keySet());
        LOG.info("HEALT PULSE called. registered services: {}", serviceNames);

        return Flux.interval(Duration.ofSeconds(3))
                .publishOn(Schedulers.boundedElastic())
                .map(tick -> {
                    String serviceName = serviceNames.get((int) (tick % serviceNames.size()));
                    EndpointURL endpoint = endpoints.get(serviceName);

                    long startTime = System.currentTimeMillis();
                    String status = HealthCheckUtil.isHealthy(endpoint) ? "OK" : "WARNING";
                    long latency = System.currentTimeMillis() - startTime;

                    return Event.of(Map.of(
                            "service", serviceName,
                            "status", status,
                            "latency", latency + "ms",
                            "timestamp", System.currentTimeMillis()
                    ));
                });
    }

}

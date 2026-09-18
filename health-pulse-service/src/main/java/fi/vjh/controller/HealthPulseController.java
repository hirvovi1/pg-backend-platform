package fi.vjh.controller;

import fi.vjh.domain.EndpointURL;
import fi.vjh.util.HealthCheckUtil;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.sse.Event;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller("/api/pulse")
public class HealthPulseController {

    private final Map<String, EndpointURL> endpoints = Map.of(
            "Java account api", EndpointURL.of("http://pgapi:8080/accounts"),
            "currency service", EndpointURL.of("http://usd-currency-service:8090/api/usd/convert?amountInCents=100"),
            "H2 db (via api)", EndpointURL.of("http://pgapi:8080/db/health")
    );


    @Get(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM)
    public Publisher<Event<Map<String, Object>>> getHealthStream() {
        List<String> serviceNames = new ArrayList<>(endpoints.keySet());

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

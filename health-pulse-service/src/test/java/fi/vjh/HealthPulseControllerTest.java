package fi.vjh;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.sse.SseClient; // TÄRKEÄ: SSE-testaukseen tarkoitettu client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "micronaut.server.port", value = "-1")
class HealthPulseControllerTest {

    @Inject
    @Client("/")
    SseClient sseClient;

    @Test
    void testHealthStreamEmitsEvents() {
        HttpRequest<?> request = HttpRequest.GET("/api/pulse/stream");

        List<Map<String, Object>> events = Flux.from(sseClient.eventStream(request, Map.class))
                .map(event -> (Map<String, Object>) event.getData())
                .take(2)
                .collectList()
                .block();

        assertNotNull(events);
        assertEquals(2, events.size());

        Map<String, Object> firstEvent = events.get(0);
        assertTrue(firstEvent.containsKey("service"));
        assertTrue(firstEvent.containsKey("status"));
        assertTrue(firstEvent.containsKey("latency"));
        assertTrue(firstEvent.containsKey("timestamp"));
    }
}

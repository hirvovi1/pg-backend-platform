package fi.vjh.facade;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.sse.Event;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

import java.util.Map;

@Singleton
public class DefaultHealthPulseServiceClient implements HealthPulseServiceClient {

    private final HttpClient client;

    public DefaultHealthPulseServiceClient(@Client("http://health-pulse-service:8095") HttpClient client) {
        this.client = client;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Publisher<Event<Map<String, Object>>> getHealthStream() {
        return (Publisher<Event<Map<String, Object>>>) (Publisher<?>) client.retrieve(
                HttpRequest.GET("/api/pulse/stream").accept(MediaType.TEXT_EVENT_STREAM),
                Argument.of(Event.class)
        );
    }
}

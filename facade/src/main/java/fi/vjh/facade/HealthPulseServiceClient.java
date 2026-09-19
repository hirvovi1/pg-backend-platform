package fi.vjh.facade;

import io.micronaut.http.sse.Event;
import org.reactivestreams.Publisher;

import java.util.Map;

public interface HealthPulseServiceClient {

    Publisher<Event<Map<String, Object>>> getHealthStream();
}

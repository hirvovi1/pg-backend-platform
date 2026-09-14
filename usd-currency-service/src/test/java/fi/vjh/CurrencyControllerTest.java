package fi.vjh;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest// Pystyttää automaattisesti Micronaut-ympäristön testiä varten
@Property(name = "micronaut.server.port", value = "-1")
class CurrencyControllerTest {

    @Inject
    @Client("/") // Injektoidaan HTTP-asiakas, joka osoittaa tähän sovellukseen
    HttpClient client;

    @Test
    void testConvertToUsd() {

        String uri = "/api/usd/convert?amountInCents=1000";

        Map<?, ?> response = client.toBlocking().retrieve(HttpRequest.GET(uri), Map.class);

        assertNotNull(response);
        assertEquals("USD", response.get("currency"));
        assertEquals(1.08, response.get("exchangeRate"));
        assertEquals("10.80", response.get("convertedAmount"));
    }
}

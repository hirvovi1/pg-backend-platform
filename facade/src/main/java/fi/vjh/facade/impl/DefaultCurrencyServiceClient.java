package fi.vjh.facade.impl;

import fi.vjh.facade.CurrencyServiceClient;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Singleton;

import java.util.Map;

@Singleton
public class DefaultCurrencyServiceClient implements CurrencyServiceClient {

    private final HttpClient client;

    public DefaultCurrencyServiceClient(@Client("http://usd-currency-service:8090") HttpClient client) {
        this.client = client;
    }

    @Override
    public Map<String, Object> convertToUsd(double amountInCents) {
        return client.toBlocking().retrieve(
                HttpRequest.GET("/api/usd/convert?amountInCents=" + amountInCents),
                Argument.mapOf(String.class, Object.class)
        );
    }
}

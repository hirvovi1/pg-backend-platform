package fi.vjh;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.Options;
import io.micronaut.http.HttpResponse;
import io.micronaut.runtime.event.annotation.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@Controller("/api/usd")
public class CurrencyController {

    private static final Logger LOG = LoggerFactory.getLogger(CurrencyController.class);

    @Value("${app.version}")
    private String version;

    @EventListener
    public void onEvent(StartupEvent event) {
        LOG.info("USD currency service version {}", version);
    }

    @Get("/convert")
    public HttpResponse<Map<String, Object>> convertToUsd(@QueryValue double amountInCents) {
        double exchangeRate = 1.08; // Simuloitu kurssi (1 EUR = 1.08 USD)
        double amountInEuros = amountInCents / 100.0;
        double amountInUsd = amountInEuros * exchangeRate;

        Map<String, Object> response = Map.of(
                "originalAmountInCents", amountInCents,
                "currency", "USD",
                "exchangeRate", exchangeRate,
                "convertedAmount", String.format("%.2f", amountInUsd)
        );

        return HttpResponse.ok(response)
                .header("Access-Control-Allow-Origin", "*");
    }

    @Options("/convert")
    public HttpResponse<?> options() {
        return HttpResponse.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type");
    }
}

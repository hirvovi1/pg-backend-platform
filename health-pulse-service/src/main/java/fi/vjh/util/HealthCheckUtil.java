package fi.vjh.util;

import fi.vjh.domain.EndpointURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public final class HealthCheckUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckUtil.class);

    private HealthCheckUtil() {
    }

    public static boolean isHealthy(EndpointURL endpoint) {
        return pingHttp(endpoint.value());
    }

    private static boolean pingHttp(String urlString) {
        try {
            LOG.debug("Pinging URL {}", urlString);
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1500);
            connection.setReadTimeout(1500);
            return connection.getResponseCode() == 200;
        } catch (IOException e) {
            LOG.error("Health check failed for URL {}", urlString, e);
            return false;
        }
    }

}

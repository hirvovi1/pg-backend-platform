package fi.vjh.util;

import fi.vjh.domain.EndpointURL;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.function.Predicate;

public final class HealthCheckUtil {



    private HealthCheckUtil() {
    }

    public static boolean isHealthy(EndpointURL endpoint) {
        return pingHttp(endpoint.value());
    }

    private static boolean pingHttp(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1500);
            connection.setReadTimeout(1500);
            return connection.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean pingDatabase(String jdbcUrl) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}

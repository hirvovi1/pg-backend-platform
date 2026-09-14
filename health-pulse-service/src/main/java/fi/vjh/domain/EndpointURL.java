package fi.vjh.domain;

public record EndpointURL(String value) {
    public static EndpointURL of(String value) {
        return new EndpointURL(value);
    }
}

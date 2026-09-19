package fi.vjh.facade;

import java.util.Map;

public interface CurrencyServiceClient {

    Map<String, Object> convertToUsd(double amountInCents);
}

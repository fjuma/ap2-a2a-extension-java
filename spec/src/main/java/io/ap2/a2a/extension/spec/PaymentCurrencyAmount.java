package io.ap2.a2a.extension.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * A PaymentCurrencyAmount is used to supply monetary amounts.
 * <p>
 * Specification:
 * https://www.w3.org/TR/payment-request/#dom-paymentcurrencyamount
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentCurrencyAmount(
        /**
         * The three-letter ISO 4217 currency code.
         */
        String currency,

        /**
         * The monetary value.
         */
        Double value
) {

    public PaymentCurrencyAmount {
        Assert.checkNotNullParam("currency", currency);
        Assert.checkNotNullParam("value", value);
    }
}

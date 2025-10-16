package io.ap2.a2a.extension.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Describes a shipping option.
 * <p>
 * Specification:
 * https://www.w3.org/TR/payment-request/#dom-paymentshippingoption
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentShippingOption(
        /**
         * A unique identifier for the shipping option.
         */
        String id,

        /**
         * A human-readable description of the shipping option.
         */
        String label,

        /**
         * The cost of this shipping option.
         */
        PaymentCurrencyAmount amount,

        /**
         * If true, indicates this as the default option.
         */
        Boolean selected
) {

    public PaymentShippingOption {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("label", label);
        Assert.checkNotNullParam("amount", amount);
        if (selected == null) {
            selected = false;
        }
    }
}

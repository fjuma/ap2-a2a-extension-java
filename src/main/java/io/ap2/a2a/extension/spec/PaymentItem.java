package io.ap2.a2a.extension.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * An item for purchase and the value asked for it.
 * <p>
 * Specification:
 * https://www.w3.org/TR/payment-request/#dom-paymentitem
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentItem(
        /**
         * A human-readable description of the item.
         */
        String label,

        /**
         * The monetary amount of the item.
         */
        PaymentCurrencyAmount amount,

        /**
         * If true, indicates the amount is not final.
         */
        Boolean pending,

        /**
         * The refund duration for this item, in days.
         */
        @JsonProperty("refund_period") Integer refundPeriod
) {

    public PaymentItem {
        Assert.checkNotNullParam("label", label);
        Assert.checkNotNullParam("amount", amount);
        if (refundPeriod == null) {
            refundPeriod = 30;
        }
    }
}

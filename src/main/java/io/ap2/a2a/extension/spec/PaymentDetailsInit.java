package io.ap2.a2a.extension.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * Contains the details of the payment being requested.
 * <p>
 * Specification:
 * https://www.w3.org/TR/payment-request/#dom-paymentdetailsinit
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentDetailsInit(
        /**
         * A unique identifier for the payment request.
         */
        String id,

        /**
         * A list of payment items to be displayed to the user.
         */
        @JsonProperty("display_items") List<PaymentItem> displayItems,

        /**
         * A list of available shipping options.
         */
        @JsonProperty("shipping_options") List<PaymentShippingOption> shippingOptions,

        /**
         * A list of price modifiers for particular payment methods.
         */
        List<PaymentDetailsModifier> modifiers,

        /**
         * The total payment amount.
         */
        PaymentItem total
) {

    public PaymentDetailsInit {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("displayItems", displayItems);
        Assert.checkNotNullParam("total", total);
    }
}

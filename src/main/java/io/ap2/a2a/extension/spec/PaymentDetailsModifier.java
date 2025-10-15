package io.ap2.a2a.extension.spec;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * Provides details that modify the payment details based on a payment method.
 * <p>
 * Specification:
 * https://www.w3.org/TR/payment-request/#dom-paymentdetailsmodifier
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentDetailsModifier(
        /**
         * The payment method ID that this modifier applies to.
         */
        @JsonProperty("supported_methods") String supportedMethods,

        /**
         * A PaymentItem value that overrides the original item total.
         */
        PaymentItem total,

        /**
         * Additional PaymentItems applicable for this payment method.
         */
        @JsonProperty("additional_display_items") List<PaymentItem> additionalDisplayItems,

        /**
         * Payment method specific data for the modifier.
         */
        Map<String, Object> data
) {

    public PaymentDetailsModifier {
        Assert.checkNotNullParam("supportedMethods", supportedMethods);
    }
}

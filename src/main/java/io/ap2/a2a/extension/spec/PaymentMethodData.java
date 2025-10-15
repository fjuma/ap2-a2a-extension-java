package io.ap2.a2a.extension.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * Indicates a payment method and associated data specific to the method.
 * <p>
 * For example:
 * <ul>
 * <li>A card may have a processing fee if it is used.</li>
 * <li>A loyalty card may offer a discount on the purchase.</li>
 * </ul>
 * <p>
 * Specification:
 * https://www.w3.org/TR/payment-request/#dom-paymentmethoddata
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentMethodData(
        /**
         * A string identifying the payment method.
         */
        @JsonProperty("supported_methods") String supportedMethods,

        /**
         * Payment method specific details.
         */
        Map<String, Object> data
) {

    public PaymentMethodData {
        Assert.checkNotNullParam("supportedMethods", supportedMethods);
        if (data == null) {
            data = Map.of();
        }
    }
}

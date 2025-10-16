package io.ap2.a2a.extension.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * A request for payment.
 * <p>
 * Specification:
 * https://www.w3.org/TR/payment-request/#paymentrequest-interface
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentRequest(
        /**
         * A list of supported payment methods.
         */
        @JsonProperty("method_data") List<PaymentMethodData> methodData,

        /**
         * The financial details of the transaction.
         */
        PaymentDetailsInit details,

        PaymentOptions options,

        /**
         * The user's provided shipping address.
         */
        @JsonProperty("shipping_address") ContactAddress shippingAddress
) {

    public PaymentRequest {
        Assert.checkNotNullParam("methodData", methodData);
        Assert.checkNotNullParam("details", details);
    }
}

package io.ap2.a2a.extension.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Information about the eligible payment options for the payment request.
 * <p>
 * Specification:
 * https://www.w3.org/TR/payment-request/#dom-paymentoptions
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentOptions(
        /**
         * Indicates if the payer's name should be collected.
         */
        @JsonProperty("request_payer_name") Boolean requestPayerName,

        /**
         * Indicates if the payer's email should be collected.
         */
        @JsonProperty("request_payer_email") Boolean requestPayerEmail,

        /**
         * Indicates if the payer's phone number should be collected.
         */
        @JsonProperty("request_payer_phone") Boolean requestPayerPhone,

        /**
         * Indicates if the payer's shipping address should be collected.
         */
        @JsonProperty("request_shipping") Boolean requestShipping,

        /**
         * Can be `shipping`, `delivery`, or `pickup`.
         */
        @JsonProperty("shipping_type") String shippingType
) {

    public PaymentOptions {
        if (requestPayerName == null) {
            requestPayerName = false;
        }
        if (requestPayerEmail == null) {
            requestPayerEmail = false;
        }
        if (requestPayerPhone == null) {
            requestPayerPhone = false;
        }
        if (requestShipping == null) {
            requestShipping = true;
        }
    }
}

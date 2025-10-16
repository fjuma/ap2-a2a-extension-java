package io.ap2.a2a.extension.spec;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * The data contents of a PaymentMandate.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentMandateContents(
        /**
         * A unique identifier for this payment mandate.
         */
        @JsonProperty("payment_mandate_id") String paymentMandateId,

        /**
         * A unique identifier for the payment request.
         */
        @JsonProperty("payment_details_id") String paymentDetailsId,

        /**
         * The total payment amount.
         */
        @JsonProperty("payment_details_total") PaymentItem paymentDetailsTotal,

        /**
         * The payment response containing details of the payment method chosen
         * by the user.
         */
        @JsonProperty("payment_response") PaymentResponse paymentResponse,

        /**
         * Identifier for the merchant.
         */
        @JsonProperty("merchant_agent") String merchantAgent,

        /**
         * The date and time the mandate was created, in ISO 8601 format.
         */
        String timestamp
) {

    public PaymentMandateContents {
        Assert.checkNotNullParam("paymentMandateId", paymentMandateId);
        Assert.checkNotNullParam("paymentDetailsId", paymentDetailsId);
        Assert.checkNotNullParam("paymentDetailsTotal", paymentDetailsTotal);
        Assert.checkNotNullParam("paymentResponse", paymentResponse);
        Assert.checkNotNullParam("merchantAgent", merchantAgent);
        if (timestamp == null) {
            timestamp = Instant.now().toString();
        }
    }
}

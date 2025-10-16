package io.ap2.a2a.extension.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * Contains the user's instructions & authorization for payment.
 * <p>
 * While the Cart and Intent mandates are required by the merchant to fulfill the
 * order, separately the protocol provides additional visibility into the agentic
 * transaction to the payments ecosystem. For this purpose, the PaymentMandate
 * (bound to Cart/Intent mandate but containing separate information) may be
 * shared with the network/issuer along with the standard transaction
 * authorization messages. The goal of the PaymentMandate is to help the
 * network/issuer build trust into the agentic transaction.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentMandate(
        /**
         * The data contents of the payment mandate.
         */
        @JsonProperty("payment_mandate_contents") PaymentMandateContents paymentMandateContents,

        /**
         * This is a base64_url-encoded verifiable presentation of a verifiable
         * credential signing over the cart_mandate and payment_mandate_hashes.
         * For example an sd-jwt-vc would contain:
         * <p>
         * - An issuer-signed jwt authorizing a 'cnf' claim<br>
         * - A key-binding jwt with the claims<br>
         *   "aud": ...<br>
         *   "nonce": ...<br>
         *   "sd_hash": hash of the issuer-signed jwt<br>
         *   "transaction_data": an array containing the secure hashes of
         *     CartMandate and PaymentMandateContents.
         */
        @JsonProperty("user_authorization") String userAuthorization
) {

    public PaymentMandate {
        Assert.checkNotNullParam("paymentMandateContents", paymentMandateContents);
    }
}

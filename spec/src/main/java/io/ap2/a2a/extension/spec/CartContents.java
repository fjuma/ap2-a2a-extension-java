package io.ap2.a2a.extension.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * The detailed contents of a cart.
 * <p>
 * This object is signed by the merchant to create a CartMandate.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CartContents(
        /**
         * A unique identifier for this cart.
         */
        String id,

        /**
         * If true, the merchant requires the user to confirm the cart before
         * the purchase can be completed.
         */
        @JsonProperty("user_cart_confirmation_required") Boolean userCartConfirmationRequired,

        /**
         * The W3C PaymentRequest object to initiate payment. This contains the
         * items being purchased, prices, and the set of payment methods
         * accepted by the merchant for this cart.
         */
        @JsonProperty("payment_request") PaymentRequest paymentRequest,

        /**
         * When this cart expires, in ISO 8601 format.
         */
        @JsonProperty("cart_expiry") String cartExpiry,

        /**
         * The name of the merchant.
         */
        @JsonProperty("merchant_name") String merchantName
) {

    public CartContents {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("userCartConfirmationRequired", userCartConfirmationRequired);
        Assert.checkNotNullParam("paymentRequest", paymentRequest);
        Assert.checkNotNullParam("cartExpiry", cartExpiry);
        Assert.checkNotNullParam("merchantName", merchantName);
    }
}

package io.ap2.a2a.extension.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * A cart whose contents have been digitally signed by the merchant.
 * <p>
 * This serves as a guarantee of the items and price for a limited time.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CartMandate(
        /**
         * The contents of the cart.
         */
        CartContents contents,

        /**
         * A base64url-encoded JSON Web Token (JWT) that digitally
         * signs the cart contents, guaranteeing its authenticity and integrity:
         * <ol>
         * <li>Header includes the signing algorithm and key ID.</li>
         * <li>Payload includes:
         *   <ul>
         *   <li>iss, sub, aud: Identifiers for the merchant (issuer)
         *     and the intended recipient (audience), like a payment processor.</li>
         *   <li>iat, exp: Timestamps for the token's creation and its
         *     short-lived expiration (e.g., 5-15 minutes) to enhance security.</li>
         *   <li>jti: Unique identifier for the JWT to prevent replay attacks.</li>
         *   <li>cart_hash: A secure hash of the CartMandate, ensuring
         *     integrity. The hash is computed over the canonical JSON
         *     representation of the CartContents object.</li>
         *   </ul>
         * </li>
         * <li>Signature: A digital signature created with the merchant's private
         *   key. It allows anyone with the public key to verify the token's
         *   authenticity and confirm that the payload has not been tampered with.</li>
         * </ol>
         * The entire JWT is base64url encoded to ensure safe transmission.
         */
        @JsonProperty("merchant_authorization") String merchantAuthorization
) {

    public CartMandate {
        Assert.checkNotNullParam("contents", contents);
    }
}

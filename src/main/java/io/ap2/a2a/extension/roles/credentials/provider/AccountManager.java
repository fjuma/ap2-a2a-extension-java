package io.ap2.a2a.extension.roles.credentials.provider;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.ap2.a2a.extension.spec.AP2Exception;
import io.ap2.a2a.extension.spec.ContactAddress;
import io.ap2.a2a.extension.spec.PaymentMethodData;

/**
 * A manager of a user's 'account details'.
 * Each 'account' contains a user's payment methods and shipping address.
 */
public interface AccountManager {

    /**
     * Creates and stores a token for an account.
     *
     * @param emailAddress The email address of the account
     * @param paymentMethodAlias The alias of the payment method
     * @return the token for the payment method
     */
    public String createToken(String emailAddress, String paymentMethodAlias);

    /**
     * Updates the token with the payment mandate id.
     *
     * @param token The token to update
     * @param paymentMandateId The payment mandate id to associate with the token
     * @throws AP2Exception if the given token is not found
     */
    public void updateToken(String token, String paymentMandateId);

    /**
     * Look up an account by token.
     *
     * @param token The token for look up
     * @param paymentMandateId The payment mandate id associated with the token
     * @return The payment method for the given token
     * @throws AP2Exception if the token is not valid
     */
    public PaymentMethodData verifyToken(String token, String paymentMandateId);

    /**
     * Returns a list of the payment methods for the given account email address.
     *
     * @param emailAddress The account's email address
     * @return A list of the user's payment methods or {@code null} if there are no payment methods for the given
     * account
     */
    public List<PaymentMethodData> getAccountPaymentMethods(String emailAddress);

    /**
     * Gets the shipping address for the given account email address.
     *
     * @param emailAddress The account's email address
     * @return The account's shipping address or {@code null} if the account doesn't exist or doesn't having a shipping
     * address set
     */
    public ContactAddress getAccountShippingAddress(String emailAddress);

    /**
     * Returns the payment method for a given account and alias.
     *
     * @param emailAddress The account's email address
     * @param alias The alias of the payment method to retrieve
     * @return The payment method for the given account and alias, or null if not found
     */
    public PaymentMethodData getPaymentMethodByAlias(String emailAddress, String alias);

}

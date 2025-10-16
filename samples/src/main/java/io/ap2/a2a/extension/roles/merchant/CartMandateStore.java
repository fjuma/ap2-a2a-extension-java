package io.ap2.a2a.extension.roles.merchant;

import io.ap2.a2a.extension.spec.CartMandate;

import java.util.Optional;

/**
 * Storage for CartMandates.
 *
 * A CartMandate may be updated multiple times during the course of a shopping
 * journey. This storage system is used to persist CartMandates between
 * interactions between the shopper and merchant agents.
 */
public interface CartMandateStore {

    /**
     * Get a cart mandate by cart ID.
     *
     * @param cartId the cart ID
     * @return the cart mandate, or null if not found
     */
    CartMandate getCartMandate(String cartId);

    /**
     * Set a cart mandate by cart ID.
     *
     * @param cartId the cart ID
     * @param cartMandate the cart mandate to store
     */
    void setCartMandate(String cartId, CartMandate cartMandate);

    /**
     * Set risk data by context ID.
     *
     * @param contextId the context ID
     * @param riskData the risk data to store
     */
    void setRiskData(String contextId, String riskData);

    /**
     * Get risk data by context ID.
     *
     * @param contextId the context ID
     * @return the risk data, or null if not found
     */
    String getRiskData(String contextId);
}

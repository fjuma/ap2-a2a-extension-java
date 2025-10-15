package io.ap2.a2a.extension.roles.merchant;

import io.ap2.a2a.extension.spec.CartMandate;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory implementation of {@link CartMandateStore}.
 *
 * This implementation uses concurrent hash maps to provide thread-safe storage
 * for cart mandates and risk data.
 */
public class InMemoryCartMandateStore implements CartMandateStore {

    private final ConcurrentMap<String, CartMandate> cartMandateStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> riskDataStore = new ConcurrentHashMap<>();

    @Override
    public CartMandate getCartMandate(String cartId) {
        return cartMandateStore.get(cartId);
    }

    @Override
    public void setCartMandate(String cartId, CartMandate cartMandate) {
        cartMandateStore.put(cartId, cartMandate);
    }

    @Override
    public void setRiskData(String contextId, String riskData) {
        riskDataStore.put(contextId, riskData);
    }

    @Override
    public String getRiskData(String contextId) {
        return riskDataStore.get(contextId);
    }
}

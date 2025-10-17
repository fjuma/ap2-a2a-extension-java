package io.ap2.a2a.extension.roles.shopping;

import io.ap2.a2a.extension.common.A2aExtensionUtils;
import io.ap2.a2a.extension.common.PaymentRemoteA2aClient;

import java.util.Set;

/**
 * Clients used by the shopping agent to communicate with remote agents.
 * <p>
 * Clients request activation of the Agent Payments Protocol extension by including
 * the X-A2A-Extensions header in each HTTP request.
 * <p>
 * This registry serves as the initial allowlist of remote agents that the shopping
 * agent trusts.
 */
public class RemoteClientRegistry {

    /**
     * Client for the credentials provider agent.
     */
    public static final PaymentRemoteA2aClient CREDENTIALS_PROVIDER_CLIENT = new PaymentRemoteA2aClient(
            "credentials_provider",
            "http://localhost:8002/a2a/credentials_provider",
            Set.of(A2aExtensionUtils.EXTENSION_URI)
    );

    /**
     * Client for the merchant agent.
     */
    public static final PaymentRemoteA2aClient MERCHANT_AGENT_CLIENT = new PaymentRemoteA2aClient(
            "merchant_agent",
            "http://localhost:8001/a2a/merchant_agent",
            Set.of(A2aExtensionUtils.EXTENSION_URI)
    );

    private RemoteClientRegistry() {
        // Utility class should not be instantiated
    }
}

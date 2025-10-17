package io.ap2.a2a.extension.common;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.JdkA2AHttpClient;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;
import io.a2a.client.transport.spi.interceptors.PayloadAndHeaders;
import io.a2a.common.A2AHeaders;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.A2AClientJSONError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Wrapper for the A2A client.
 * <p>
 * Always assumes the AgentCard is at base_url + /.well-known/agent-card.json.
 * <p>
 * Provides convenience for establishing connection and for sending messages.
 */
public class PaymentRemoteA2aClient {

    private final A2AHttpClient httpClient;
    private final String name;
    private final String baseUrl;
    private final Set<String> requiredExtensions;
    private AgentCard agentCard;

    /**
     * Initializes the PaymentRemoteA2aClient.
     *
     * @param name the name of the agent
     * @param baseUrl the base URL where the remote agent is hosted
     * @param requiredExtensions a set of extension URIs that the client requires
     */
    public PaymentRemoteA2aClient(String name, String baseUrl, @Nullable Set<String> requiredExtensions) {
        this.httpClient = new JdkA2AHttpClient();
        this.name = name;
        this.baseUrl = baseUrl;
        this.requiredExtensions = requiredExtensions != null ? requiredExtensions : Set.of();
        this.agentCard = null;
    }

    /**
     * Get agent card.
     *
     * @return the agent card
     * @throws A2AClientError if an HTTP error occurs fetching the card
     * @throws A2AClientJSONError if the response body cannot be decoded as JSON
     */
    public AgentCard getAgentCard() throws A2AClientError, A2AClientJSONError {
        if (agentCard == null) {
            agentCard = A2A.getAgentCard(httpClient, baseUrl);
        }
        return agentCard;
    }

    /**
     * Retrieves the A2A client.
     *
     * @param consumers the event consumers for handling client events
     * @return the A2A client
     * @throws A2AClientError if client creation fails
     * @throws A2AClientJSONError if the agent card cannot be decoded
     * @throws A2AClientException if the client cannot be built
     */
    public Client getA2aClient(List<BiConsumer<ClientEvent, AgentCard>> consumers)
            throws A2AClientError, A2AClientJSONError, A2AClientException {

        AgentCard card = getAgentCard();

        // Create an interceptor to add the X-A2A-Extensions header
        ExtensionHeaderInterceptor extensionInterceptor = new ExtensionHeaderInterceptor(requiredExtensions);

        return Client.builder(card)
                .withTransport(JSONRPCTransport.class,
                        new JSONRPCTransportConfigBuilder()
                                .httpClient(httpClient)
                                .addInterceptor(extensionInterceptor)
                                .build())
                .addConsumers(consumers)
                .clientConfig(new ClientConfig.Builder().build())
                .build();
    }

    /**
     * Get the name of the remote agent.
     *
     * @return the agent name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the base URL of the remote agent.
     *
     * @return the base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Interceptor that adds the X-A2A-Extensions header to requests.
     */
    private static class ExtensionHeaderInterceptor extends ClientCallInterceptor {

        private final String extensionHeaderValue;

        public ExtensionHeaderInterceptor(Set<String> requiredExtensions) {
            this.extensionHeaderValue = String.join(", ", requiredExtensions);
        }

        @Override
        public PayloadAndHeaders intercept(String methodName, @Nullable Object payload,
                                          Map<String, String> headers, AgentCard agentCard,
                                          @Nullable ClientCallContext clientCallContext) {
            Map<String, String> updatedHeaders = new HashMap<>(headers != null ? headers : Map.of());

            if (!extensionHeaderValue.isEmpty()) {
                updatedHeaders.put(A2AHeaders.X_A2A_EXTENSIONS, extensionHeaderValue);
            }

            return new PayloadAndHeaders(payload, updatedHeaders);
        }
    }
}

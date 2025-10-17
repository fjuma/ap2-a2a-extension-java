package io.ap2.a2a.extension.roles.shopping.subagents.payment.method.collector;

import static io.ap2.a2a.extension.spec.AP2Constants.PAYMENT_METHOD_DATA_DATA_KEY;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.DataPart;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.ap2.a2a.extension.common.A2aMessageBuilder;
import io.ap2.a2a.extension.spec.AP2Exception;
import io.ap2.a2a.extension.spec.CartMandate;
import io.ap2.a2a.extension.spec.PaymentMethodData;

/**
 * Tools used by the payment method collector subagent.
 * <p>
 * Each agent uses individual tools to handle distinct tasks throughout the
 * shopping and purchasing process.
 */
public class Tools {

    private static final Logger logger = Logger.getLogger(Tools.class.getName());

    /**
     * Gets the user's payment methods from the credentials provider.
     * <p>
     * These will match the payment method on the cart being purchased.
     *
     * @param userEmail The user's email address.
     * @param state The state map for managing tool context.
     * @param credentialsProviderClient The credentials provider client.
     * @return A list of the user's applicable payment method aliases.
     * @throws AP2Exception if required state is missing or operation fails
     */
    public List<String> getPaymentMethods(
            String userEmail,
            Map<String, Object> state,
            Client credentialsProviderClient) throws AP2Exception {

        CartMandate cartMandate = (CartMandate) state.get("cart_mandate");
        if (cartMandate == null) {
            throw new AP2Exception("No cart mandate found in tool context state.");
        }

        String shoppingContextId = (String) state.get("shopping_context_id");

        A2aMessageBuilder messageBuilder = new A2aMessageBuilder()
                .setContextId(shoppingContextId)
                .addText("Get a filtered list of the user's payment methods.")
                .addData("user_email", userEmail);

        for (PaymentMethodData methodData : cartMandate.contents().paymentRequest().methodData()) {
            messageBuilder.addData(PAYMENT_METHOD_DATA_DATA_KEY, methodData.data());
        }

        List<String>[] paymentMethodsHolder = new List[1];
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();

        consumers.add((event, agentCard) -> {
            if (event instanceof TaskEvent taskEvent) {
                Task task = taskEvent.getTask();
                if (task.getArtifacts() != null && !task.getArtifacts().isEmpty()) {
                    for (Artifact artifact : task.getArtifacts()) {
                        for (Part<?> part : artifact.parts()) {
                            if (part instanceof DataPart dataPart) {
                                Object aliases = dataPart.getData().get("payment_method_aliases");
                                if (aliases instanceof List) {
                                    paymentMethodsHolder[0] = (List<String>) aliases;
                                }
                            }
                        }
                    }
                }
            }
        });

        Consumer<Throwable> errorHandler = throwable -> {
            logger.severe("Error getting payment methods: " + throwable.getMessage());
        };

        try {
            credentialsProviderClient.sendMessage(messageBuilder.build(), consumers, errorHandler, null);
        } catch (Exception e) {
            throw new AP2Exception("Failed to get payment methods: " + e.getMessage(), e);
        }

        if (paymentMethodsHolder[0] == null) {
            throw new AP2Exception("Failed to get payment methods.");
        }

        return paymentMethodsHolder[0];
    }

    /**
     * Gets a payment credential token from the credentials provider.
     *
     * @param userEmail The user's email address.
     * @param paymentMethodAlias The payment method alias.
     * @param state The state map for managing tool context.
     * @param credentialsProviderClient The credentials provider client.
     * @return A map with status and token information.
     * @throws AP2Exception if required state is missing or operation fails
     */
    public Map<String, Object> getPaymentCredentialToken(
            String userEmail,
            String paymentMethodAlias,
            Map<String, Object> state,
            Client credentialsProviderClient) throws AP2Exception {

        String shoppingContextId = (String) state.get("shopping_context_id");

        A2aMessageBuilder messageBuilder = new A2aMessageBuilder()
                .setContextId(shoppingContextId)
                .addText("Get a payment credential token for the user's payment method.")
                .addData("payment_method_alias", paymentMethodAlias)
                .addData("user_email", userEmail);

        String[] tokenHolder = new String[1];
        String[] urlHolder = new String[1];
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();

        consumers.add((event, agentCard) -> {
            if (event instanceof TaskEvent taskEvent) {
                Task task = taskEvent.getTask();
                if (task.getArtifacts() != null && !task.getArtifacts().isEmpty()) {
                    for (Artifact artifact : task.getArtifacts()) {
                        for (Part<?> part : artifact.parts()) {
                            if (part instanceof DataPart dataPart) {
                                Object token = dataPart.getData().get("token");
                                if (token instanceof String) {
                                    tokenHolder[0] = (String) token;
                                }
                            }
                        }
                    }
                }
            }
            if (agentCard != null) {
                urlHolder[0] = agentCard.url();
            }
        });

        Consumer<Throwable> errorHandler = throwable -> {
            logger.severe("Error getting payment credential token: " + throwable.getMessage());
        };

        try {
            credentialsProviderClient.sendMessage(messageBuilder.build(), consumers, errorHandler, null);
        } catch (Exception e) {
            throw new AP2Exception("Failed to get payment credential token: " + e.getMessage(), e);
        }

        if (tokenHolder[0] == null) {
            throw new AP2Exception("Failed to get payment credential token.");
        }

        Map<String, Object> paymentCredentialToken = Map.of(
                "value", tokenHolder[0],
                "url", urlHolder[0] != null ? urlHolder[0] : ""
        );

        state.put("payment_credential_token", paymentCredentialToken);

        return Map.of("status", "success", "token", tokenHolder[0]);
    }
}

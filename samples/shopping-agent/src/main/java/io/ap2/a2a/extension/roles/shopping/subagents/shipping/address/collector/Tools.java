package io.ap2.a2a.extension.roles.shopping.subagents.shipping.address.collector;

import static io.ap2.a2a.extension.spec.AP2Constants.CONTACT_ADDRESS_DATA_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

import dev.langchain4j.agent.tool.Tool;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.Task;
import io.ap2.a2a.extension.common.A2aMessageBuilder;
import io.ap2.a2a.extension.common.ArtifactUtils;
import io.ap2.a2a.extension.roles.shopping.RemoteClientRegistry;
import io.ap2.a2a.extension.spec.AP2Exception;
import io.ap2.a2a.extension.spec.ContactAddress;
import jakarta.enterprise.context.RequestScoped;

/**
 * Tools used by the shipping address collector subagent.
 * <p>
 * Each agent uses individual tools to handle distinct tasks throughout the
 * shopping and purchasing process.
 * <p>
 * This class provides LangChain4j @Tool annotated methods for AI agent invocation.
 */
@RequestScoped
public class Tools {

    private static final Logger logger = Logger.getLogger(Tools.class.getName());

    private final Map<String, Object> state = new HashMap<>();
    private Client credentialsProviderClient;

    /**
     * Gets or creates the credentials provider client from the registry.
     *
     * @return the credentials provider client
     */
    private Client getCredentialsProviderClient() {
        if (credentialsProviderClient == null) {
            try {
                credentialsProviderClient = RemoteClientRegistry.CREDENTIALS_PROVIDER_CLIENT.getA2aClient(List.of());
            } catch (Exception e) {
                throw new RuntimeException("Failed to create credentials provider client", e);
            }
        }
        return credentialsProviderClient;
    }

    /**
     * Gets the user's shipping address from the credentials provider.
     *
     * @param userEmail The ID of the user to get the shipping address for.
     * @return The user's shipping address.
     */
    @Tool("Get the user's shipping address from their digital wallet")
    public ContactAddress getShippingAddress(String userEmail) {

        String shoppingContextId = (String) state.get("shopping_context_id");

        A2aMessageBuilder messageBuilder = new A2aMessageBuilder()
                .setContextId(shoppingContextId)
                .addText("Get the user's shipping address.")
                .addData("user_email", userEmail);

        ContactAddress[] addressHolder = new ContactAddress[1];
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();

        consumers.add((event, agentCard) -> {
            if (event instanceof TaskEvent taskEvent) {
                Task task = taskEvent.getTask();
                if (task.getArtifacts() != null && !task.getArtifacts().isEmpty()) {
                    List<ContactAddress> addresses = parseAddresses(task.getArtifacts());
                    if (!addresses.isEmpty()) {
                        addressHolder[0] = addresses.get(0);
                    }
                }
            }
        });

        Consumer<Throwable> errorHandler = throwable -> {
            logger.severe("Error getting shipping address: " + throwable.getMessage());
        };

        try {
            getCredentialsProviderClient().sendMessage(messageBuilder.build(), consumers, errorHandler, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get shipping address: " + e.getMessage(), e);
        }

        if (addressHolder[0] == null) {
            throw new RuntimeException("Failed to get shipping address.");
        }

        return addressHolder[0];
    }

    /**
     * Parses a list of artifacts into a list of ContactAddress objects.
     *
     * @param artifacts The list of artifacts to parse.
     * @return A list of ContactAddress objects.
     */
    private List<ContactAddress> parseAddresses(List<Artifact> artifacts) {
        return ArtifactUtils.findCanonicalObjects(artifacts, CONTACT_ADDRESS_DATA_KEY, ContactAddress.class);
    }
}

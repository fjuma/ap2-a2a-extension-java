package io.ap2.a2a.extension.roles.shopping.subagents.shopper;

import static io.ap2.a2a.extension.spec.AP2Constants.CART_MANDATE_DATA_KEY;
import static io.ap2.a2a.extension.spec.AP2Constants.INTENT_MANDATE_DATA_KEY;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import io.a2a.spec.TaskStatus;
import io.ap2.a2a.extension.common.A2aMessageBuilder;
import io.ap2.a2a.extension.common.ArtifactUtils;
import io.ap2.a2a.extension.spec.AP2Exception;
import io.ap2.a2a.extension.spec.CartMandate;
import io.ap2.a2a.extension.spec.IntentMandate;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Tools used by the shopper subagent.
 * <p>
 * Each agent uses individual tools to handle distinct tasks throughout the
 * shopping and purchasing process.
 * <p>
 * This class provides LangChain4j @Tool annotated methods for AI agent invocation.
 */
@ApplicationScoped
public class Tools {

    private static final Logger logger = Logger.getLogger(Tools.class.getName());

    private Map<String, Object> state;
    private Client merchantClient;
    private boolean debugMode;

    /**
     * Initializes the tools with required context.
     * Must be called before tools can be invoked by the AI agent.
     *
     * @param state the shared state map
     * @param merchantClient the merchant agent client
     * @param debugMode whether debug mode is enabled
     */
    public void initialize(Map<String, Object> state, Client merchantClient, boolean debugMode) {
        this.state = state;
        this.merchantClient = merchantClient;
        this.debugMode = debugMode;
    }

    /**
     * Creates an IntentMandate object.
     *
     * @param naturalLanguageDescription The description of the user's intent.
     * @param userCartConfirmationRequired If the user must confirm the cart.
     * @param merchants A list of allowed merchants.
     * @param skus A list of allowed SKUs.
     * @param requiresRefundability If the items must be refundable.
     * @return An IntentMandate object valid for 1 day.
     */
    @Tool("Create an IntentMandate that captures the user's shopping intent")
    public IntentMandate createIntentMandate(
            String naturalLanguageDescription,
            boolean userCartConfirmationRequired,
            List<String> merchants,
            List<String> skus,
            boolean requiresRefundability) {

        Instant intentExpiry = Instant.now().plus(1, ChronoUnit.DAYS);

        IntentMandate intentMandate = new IntentMandate(
                userCartConfirmationRequired,
                naturalLanguageDescription,
                merchants,
                skus,
                requiresRefundability,
                intentExpiry.toString()
        );

        state.put("intent_mandate", intentMandate);
        return intentMandate;
    }

    /**
     * Calls the merchant agent to find products matching the user's intent.
     *
     * @return A list of CartMandate objects.
     */
    @Tool("Find products from the merchant that match the user's intent")
    public List<CartMandate> findProducts() {

        IntentMandate intentMandate = (IntentMandate) state.get("intent_mandate");
        if (intentMandate == null) {
            throw new RuntimeException("No IntentMandate found in tool context state.");
        }

        String riskData = collectRiskData(state);
        if (riskData == null) {
            throw new RuntimeException("No risk data found in tool context state.");
        }

        A2aMessageBuilder messageBuilder = new A2aMessageBuilder()
                .addText("Find products that match the user's IntentMandate.")
                .addData(INTENT_MANDATE_DATA_KEY, intentMandate)
                .addData("risk_data", riskData)
                .addData("debug_mode", debugMode)
                .addData("shopping_agent_id", "trusted_shopping_agent");

        List<CartMandate>[] cartMandatesHolder = new List[1];
        String[] contextIdHolder = new String[1];
        TaskStatus[] statusHolder = new TaskStatus[1];
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();

        consumers.add((event, agentCard) -> {
            if (event instanceof TaskEvent taskEvent) {
                Task task = taskEvent.getTask();
                statusHolder[0] = task.getStatus();
                contextIdHolder[0] = task.getContextId();
                if (task.getArtifacts() != null && !task.getArtifacts().isEmpty()) {
                    cartMandatesHolder[0] = parseCartMandates(task.getArtifacts());
                }
            }
        });

        Consumer<Throwable> errorHandler = throwable -> {
            logger.severe("Error finding products: " + throwable.getMessage());
        };

        try {
            merchantClient.sendMessage(messageBuilder.build(), consumers, errorHandler, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find products: " + e.getMessage(), e);
        }

        if (statusHolder[0] == null || !"completed".equals(statusHolder[0].state().asString())) {
            throw new RuntimeException("Failed to find products: " + statusHolder[0]);
        }

        state.put("shopping_context_id", contextIdHolder[0]);
        state.put("cart_mandates", cartMandatesHolder[0]);

        return cartMandatesHolder[0];
    }

    /**
     * Updates the chosen CartMandate in the tool context state.
     *
     * @param cartId The ID of the chosen cart.
     * @return A status message.
     */
    @Tool("Update the chosen cart with the cart ID selected by the user")
    public String updateChosenCartMandate(String cartId) {
        List<CartMandate> cartMandates = (List<CartMandate>) state.get("cart_mandates");
        if (cartMandates == null) {
            cartMandates = new ArrayList<>();
        }

        for (CartMandate cart : cartMandates) {
            logger.info("Checking cart with ID: " + cart.contents().id() + " with chosen ID: " + cartId);
            if (cart.contents().id().equals(cartId)) {
                state.put("chosen_cart_id", cartId);
                return "CartMandate with ID " + cartId + " selected.";
            }
        }
        return "CartMandate with ID " + cartId + " not found.";
    }

    /**
     * Parses a list of artifacts into a list of CartMandate objects.
     *
     * @param artifacts The list of artifacts to parse.
     * @return A list of CartMandate objects.
     */
    private List<CartMandate> parseCartMandates(List<Artifact> artifacts) {
        return ArtifactUtils.findCanonicalObjects(artifacts, CART_MANDATE_DATA_KEY, CartMandate.class);
    }

    /**
     * Creates a risk_data in the tool_context.
     * <p>
     * This is a fake risk data for demonstration purposes.
     *
     * @param state The state map for managing tool context.
     * @return The risk data.
     */
    private String collectRiskData(Map<String, Object> state) {
        String riskData = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...fake_risk_data";
        state.put("risk_data", riskData);
        return riskData;
    }
}

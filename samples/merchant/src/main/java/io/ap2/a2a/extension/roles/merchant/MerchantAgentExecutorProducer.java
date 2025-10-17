package io.ap2.a2a.extension.roles.merchant;

import io.a2a.client.Client;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentExtension;
import io.a2a.spec.DataPart;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;
import io.ap2.a2a.extension.common.BaseAgentExecutor;
import io.ap2.a2a.extension.common.MessageUtils;
import io.ap2.a2a.extension.spec.AP2Exception;
import io.ap2.a2a.extension.roles.merchant.subagents.CatalogAgent;
import io.ap2.a2a.extension.roles.merchant.subagents.ItemGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Producer for merchant agent executor.
 */
@ApplicationScoped
public final class MerchantAgentExecutorProducer {

    private static final Logger logger = Logger.getLogger(
        MerchantAgentExecutorProducer.class.getName());

    /**
     * A list of known Shopping Agent identifiers that this Merchant is willing to
     * work with.
     */
    private static final Set<String> KNOWN_SHOPPING_AGENTS = Set.of(
        "trusted_shopping_agent"
    );

    /**
     * The cart mandate store instance.
     */
    @Inject
    CartMandateStore cartMandateStore;

    /**
     * The LangChain4j agent for intelligent tool selection.
     */
    @Inject
    MerchantAgent agent;

    /**
     * The LangChain4j agent for generating product items.
     */
    @Inject
    ItemGenerator itemGenerator;

    /**
     * The agent card.
     */
    @Inject
    @PublicAgentCard
    AgentCard agentCard;

    /**
     * Produces the agent executor for the merchant agent.
     *
     * @return the configured agent executor
     */
    @Produces
    public AgentExecutor agentExecutor() {
        // Could be configured via @ConfigProperty in the future
        boolean debugMode = false;

        return new MerchantAgentExecutor(
            cartMandateStore,
            agent,
            itemGenerator,
            agentCard.capabilities().extensions(),
            debugMode
        );
    }

    /**
     * Merchant agent executor implementation.
     *
     * This executor extends BaseAgentExecutor and uses LangChain4j AI-powered
     * tool selection to dispatch requests to appropriate tools. It supports the
     * Agent Payments Protocol (AP2) extension.
     *
     * This implementation follows the Python MerchantAgentExecutor pattern by:
     * 1. Accepting a list of supported A2A extensions
     * 2. Validating the shopping agent ID before processing
     * 3. Using AI (via LangChain4j) to identify the appropriate tool
     * 4. Invoking the selected tool to complete the task
     */
    private static class MerchantAgentExecutor extends BaseAgentExecutor {

        private final CartMandateStore cartMandateStore;
        private final Tools tools;
        private final boolean debugMode;
        private final CatalogAgent catalogAgent;

        /**
         * Constructor for MerchantAgentExecutor.
         *
         * @param cartMandateStore the cart mandate store instance
         * @param agent the LangChain4j agent for tool selection
         * @param itemGenerator the LangChain4j agent for generating product items
         * @param supportedExtensions the list of extensions from the agent card
         * @param debugMode whether debug mode is enabled (defaults to false)
         */
        MerchantAgentExecutor(final CartMandateStore cartMandateStore,
                              final MerchantAgent agent,
                              final ItemGenerator itemGenerator,
                              final List<AgentExtension> supportedExtensions,
                              final boolean debugMode) {
            super(supportedExtensions, agent);
            this.cartMandateStore = cartMandateStore;
            this.tools = new Tools();
            this.debugMode = debugMode;
            this.catalogAgent = new CatalogAgent(cartMandateStore, itemGenerator);
        }

        @Override
        protected void handleRequest(final List<TextPart> textParts,
                                     final List<DataPart> dataParts,
                                     final TaskUpdater updater,
                                     final Task currentTask) throws AP2Exception {
            // Validate shopping agent before processing
            // This mirrors the Python _validate_shopping_agent override
            if (!validateShoppingAgent(dataParts, updater)) {
                throw new AP2Exception("Shopping agent validation failed");
            }

            // Continue with normal request handling
            selectAndInvokeTool(textParts, dataParts, updater, currentTask);
        }

        /**
         * Validates that the incoming request is from a trusted Shopping Agent.
         * This mirrors the Python _validate_shopping_agent method.
         *
         * @param dataParts the data parts from the request
         * @param updater the task updater
         * @return true if the Shopping Agent is trusted, or false if not
         */
        private boolean validateShoppingAgent(final List<DataPart> dataParts,
                                              final TaskUpdater updater) {
            String shoppingAgentId = (String) MessageUtils.findDataPart("shopping_agent_id", dataParts);
            logger.info("Received request from shopping_agent_id: " + shoppingAgentId);

            if (shoppingAgentId == null) {
                logger.warning("Missing shopping_agent_id in request.");
                failTask(updater, "Unauthorized Request: Missing shopping_agent_id.");
                return false;
            }

            if (!KNOWN_SHOPPING_AGENTS.contains(shoppingAgentId)) {
                logger.warning("Unknown Shopping Agent: " + shoppingAgentId);
                failTask(updater, "Unauthorized Request: Unknown agent '" + shoppingAgentId + "'.");
                return false;
            }

            logger.info("Authorized request from shopping_agent_id: " + shoppingAgentId);
            return true;
        }

        /**
         * Selects and invokes the appropriate tool using AI.
         *
         * This method uses LangChain4j AI-powered tool selection to intelligently
         * determine the appropriate operation based on the user's request.
         * This mirrors the Python implementation's FunctionCallResolver.
         *
         * @param textParts the text parts from the request
         * @param dataParts the data parts from the request
         * @param updater the task updater
         * @param currentTask the current task
         * @throws AP2Exception if an error occurs during tool execution
         */
        private void selectAndInvokeTool(final List<TextPart> textParts,
                                         final List<DataPart> dataParts,
                                         final TaskUpdater updater,
                                         final Task currentTask) throws AP2Exception {
            String prompt = !textParts.isEmpty() ? textParts.get(0).getText().strip() : "";

            logger.info("Processing request: " + prompt);

            // Use the agent to determine which tool to use
            // This mirrors the Python FunctionCallResolver.determine_tool_to_use()
            String toolName = selectTool(prompt);

            // Invoke the selected tool
            // This mirrors the Python implementation's tool dispatch logic
            switch (toolName.strip()) {
                case "updateCart":
                    tools.updateCart(dataParts, updater, currentTask, cartMandateStore, debugMode);
                    break;
                case "findItemsWorkflow":
                    catalogAgent.findItemsWorkflow(dataParts, updater, currentTask);
                    break;
                case "initiatePayment":
                    tools.initiatePayment(dataParts, updater, currentTask, cartMandateStore, debugMode);
                    break;
                case "dpcFinish":
                    tools.dpcFinish(dataParts, updater, currentTask);
                    break;
                default:
                    throw new AP2Exception(
                        "Unknown tool selected: " + toolName + ". " +
                        "Unable to determine appropriate tool for request.");
            }
        }

        /**
         * A helper function to fail a task with a given error message.
         *
         * @param updater The TaskUpdater instance
         * @param errorText The error message
         */
        @Override
        protected void failTask(TaskUpdater updater, String errorText) {
            try {
                Message errorMessage = updater.newAgentMessage(
                        List.of(new TextPart(errorText)), null);
                updater.fail(errorMessage);
            } catch (Exception e) {
                logger.severe("Failed to fail task: " + e.getMessage());
            }
        }
    }
}

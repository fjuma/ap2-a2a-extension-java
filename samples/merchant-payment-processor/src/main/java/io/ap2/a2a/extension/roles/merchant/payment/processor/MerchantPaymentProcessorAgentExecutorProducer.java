package io.ap2.a2a.extension.roles.merchant.payment.processor;

import io.a2a.client.Client;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentExtension;
import io.a2a.spec.DataPart;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;
import io.ap2.a2a.extension.common.BaseAgentExecutor;
import io.ap2.a2a.extension.spec.AP2Exception;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.util.List;
import java.util.logging.Logger;

/**
 * Producer for merchant payment processor agent executor.
 */
@ApplicationScoped
public final class MerchantPaymentProcessorAgentExecutorProducer {

    private static final Logger logger = Logger.getLogger(
        MerchantPaymentProcessorAgentExecutorProducer.class.getName());

    /**
     * The LangChain4j agent for intelligent tool selection.
     */
    @Inject
    MerchantPaymentProcessorAgent agent;

    /**
     * The agent card.
     */
    @Inject
    @PublicAgentCard
    AgentCard agentCard;

    /**
     * Produces the agent executor for the merchant payment processor agent.
     *
     * @return the configured agent executor
     */
    @Produces
    public AgentExecutor agentExecutor() {
        // Could be configured via @ConfigProperty in the future
        boolean debugMode = false;

        return new MerchantPaymentProcessorAgentExecutor(
            agent,
            agentCard.capabilities().extensions(),
            debugMode
        );
    }

    /**
     * Merchant payment processor agent executor implementation.
     * This agent's role is to:
     * 1. Complete payments, engaging with the credentials provider agent when needed.
     *
     * This executor extends BaseAgentExecutor and uses LangChain4j AI-powered
     * tool selection to dispatch requests to appropriate tools. It supports the
     * Agent Payments Protocol (AP2) extension.
     *
     * This implementation follows the Python PaymentProcessorExecutor pattern by:
     * 1. Accepting a list of supported A2A extensions
     * 2. Using AI (via LangChain4j) to identify the appropriate tool
     * 3. Invoking the selected tool to complete the task
     */
    private static class MerchantPaymentProcessorAgentExecutor extends BaseAgentExecutor {

        private final Tools tools;
        private final boolean debugMode;

        /**
         * Constructor for MerchantPaymentProcessorAgentExecutor.
         *
         * @param agent the LangChain4j agent for tool selection
         * @param supportedExtensions the list of extensions from the agent card
         * @param debugMode whether debug mode is enabled (defaults to false)
         */
        MerchantPaymentProcessorAgentExecutor(
                final MerchantPaymentProcessorAgent agent,
                final List<AgentExtension> supportedExtensions,
                final boolean debugMode) {
            super(supportedExtensions, agent);
            this.tools = new Tools();
            this.debugMode = debugMode;
        }

        @Override
        protected void handleRequest(final List<TextPart> textParts,
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
            switch (toolName.trim()) {
                case "initiatePayment":
                    tools.initiatePayment(dataParts, updater, currentTask, debugMode);
                    break;
                default:
                    throw new AP2Exception(
                        "Unknown tool selected: " + toolName + ". " +
                        "Unable to determine appropriate tool for request.");
            }
        }
    }
}

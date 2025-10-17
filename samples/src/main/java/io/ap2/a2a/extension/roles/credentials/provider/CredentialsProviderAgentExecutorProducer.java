package io.ap2.a2a.extension.roles.credentials.provider;

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
import java.util.Set;
import java.util.logging.Logger;

/**
 * Producer for credentials provider agent executor.
 * This class is final and not designed for extension.
 */
@ApplicationScoped
public final class CredentialsProviderAgentExecutorProducer {

    private static final Logger logger = Logger.getLogger(
        CredentialsProviderAgentExecutorProducer.class.getName());

    /**
     * The account manager instance.
     */
    @Inject
    AccountManager accountManager;

    /**
     * The LangChain4j agent for intelligent tool selection.
     */
    @Inject
    CredentialsProviderAgent agent;

    /**
     * The agent card.
     */
    @Inject
    @PublicAgentCard
    AgentCard agentCard;

    /**
     * Produces the agent executor for the credentials provider agent.
     *
     * @return the configured agent executor
     */
    @Produces
    public AgentExecutor agentExecutor() {
        return new CredentialsProviderAgentExecutor(
            accountManager,
            agent,
            agentCard.capabilities().extensions()
        );
    }

    /**
     * Credentials provider agent executor implementation.
     *
     * This executor extends BaseAgentExecutor and uses LangChain4j AI-powered
     * tool selection to dispatch requests to appropriate tools. It supports the
     * Agent Payments Protocol (AP2) extension.
     *
     * This implementation follows the Python BaseServerExecutor pattern by:
     * 1. Accepting a list of supported A2A extensions
     * 2. Using AI (via LangChain4j) to identify the appropriate tool
     * 3. Invoking the selected tool to complete the task
     */
    private static class CredentialsProviderAgentExecutor extends BaseAgentExecutor {

        private final Tools tools;

        /**
         * Constructor for CredentialsProviderAgentExecutor.
         *
         * @param accountManager the account manager instance
         * @param agent the LangChain4j agent for tool selection
         * @param supportedExtensions the list of extensions from the agent card
         */
        CredentialsProviderAgentExecutor(final AccountManager accountManager,
                                         final CredentialsProviderAgent agent,
                                         final List<AgentExtension> supportedExtensions) {
            super(supportedExtensions, agent);
            this.tools = new Tools(accountManager);
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
                case "handleGetShippingAddress":
                    tools.handleGetShippingAddress(dataParts, updater, currentTask);
                    break;
                case "handleSearchPaymentMethods":
                    tools.handleSearchPaymentMethods(dataParts, updater, currentTask);
                    break;
                case "handleCreatePaymentCredentialToken":
                    tools.handleCreatePaymentCredentialToken(dataParts, updater, currentTask);
                    break;
                case "handleGetPaymentMethodRawCredentials":
                    tools.handleGetPaymentMethodRawCredentials(dataParts, updater, currentTask);
                    break;
                case "handleSignedPaymentMandate":
                    tools.handleSignedPaymentMandate(dataParts, updater, currentTask);
                    break;
                default:
                    throw new AP2Exception(
                        "Unknown tool selected: " + toolName + ". " +
                        "Unable to determine appropriate tool for request.");
            }
        }
    }
}

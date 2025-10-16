package io.ap2.a2a.extension.roles.credentials.provider;

import static io.ap2.a2a.extension.spec.AP2Constants.PAYMENT_MANDATE_DATA_KEY;
import static io.ap2.a2a.extension.spec.AP2Constants.PAYMENT_METHOD_DATA_DATA_KEY;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.DataPart;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import io.ap2.a2a.extension.common.A2aExtensionUtils;
import io.ap2.a2a.extension.common.MessageUtils;
import io.ap2.a2a.extension.spec.AP2Exception;
import io.ap2.a2a.extension.spec.PaymentMandate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.util.ArrayList;
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
     * Produces the agent executor for the credentials provider agent.
     *
     * @return the configured agent executor
     */
    @Produces
    public AgentExecutor agentExecutor() {
        return new CredentialsProviderAgentExecutor(accountManager, agent);
    }

    /**
     * Credentials provider agent executor implementation.
     *
     * This executor handles extension activation and uses LangChain4j AI-powered
     * tool selection to dispatch requests to appropriate tools. It supports the
     * Agent Payments Protocol (AP2) extension.
     *
     * This implementation follows the Python BaseServerExecutor pattern by:
     * 1. Accepting a list of supported A2A extensions
     * 2. Using AI (via LangChain4j) to identify the appropriate tool
     * 3. Invoking the selected tool to complete the task
     */
    private static class CredentialsProviderAgentExecutor implements AgentExecutor {

        private final Tools tools;
        private final CredentialsProviderAgent agent;
        private final Set<String> supportedExtensionUris;

        /**
         * Constructor for CredentialsProviderAgentExecutor.
         *
         * @param accountManager the account manager instance
         * @param agent the LangChain4j agent for tool selection
         */
        CredentialsProviderAgentExecutor(final AccountManager accountManager,
                                         final CredentialsProviderAgent agent) {
            this.tools = new Tools(accountManager);
            this.agent = agent;
            // Define the extensions this agent supports
            this.supportedExtensionUris = Set.of(
                A2aExtensionUtils.EXTENSION_URI,
                "https://sample-card-network.github.io/paymentmethod/types/v1"
            );
        }

        @Override
        public void execute(final RequestContext context,
                            final EventQueue eventQueue) throws JSONRPCError {
            final TaskUpdater updater = new TaskUpdater(context, eventQueue);

            // Mark the task as submitted and start working on it
            if (context.getTask() == null) {
                updater.submit();
            }
            updater.startWork();

            try {
                // Handle extension activation
                handleExtensions(context);

                // Verify that the AP2 extension is activated
                if (!context.getCallContext().getActivatedExtensions().contains(A2aExtensionUtils.EXTENSION_URI)) {
                    throw new AP2Exception(
                        "Payment extension not activated: " + context.getCallContext().getActivatedExtensions());
                }

                // Parse the request
                List<TextPart> textParts = new ArrayList<>();
                List<DataPart> dataParts = new ArrayList<>();
                parseRequest(context.getMessage(), textParts, dataParts);

                logger.info("Received request with " + textParts.size() + " text parts and "
                    + dataParts.size() + " data parts");

                // Validate payment mandate signature if present
                Object paymentMandateData = MessageUtils.findDataPart(PAYMENT_MANDATE_DATA_KEY, dataParts);
                if (paymentMandateData != null) {
                    PaymentMandate paymentMandate = MessageUtils.parseCanonicalObject(
                        PAYMENT_MANDATE_DATA_KEY, dataParts, PaymentMandate.class);
                    // TODO: Add signature validation
                    logger.info("Payment mandate received: " + paymentMandate.paymentMandateContents().paymentMandateId());
                }

                // Determine which tool to use and execute it
                // Uses LangChain4j AI agent to determine the appropriate tool,
                // mirroring the Python FunctionCallResolver approach
                handleRequest(textParts, dataParts, updater, context.getTask());

            } catch (AP2Exception e) {
                logger.severe("AP2 error: " + e.getMessage());
                updater.fail(updater.newAgentMessage(
                    List.of(new TextPart("An error occurred: " + e.getMessage())), null));
            } catch (Exception e) {
                logger.severe("Unexpected error: " + e.getMessage());
                updater.fail(updater.newAgentMessage(
                    List.of(new TextPart("An unexpected error occurred: " + e.getMessage())), null));
            }
        }

        /**
         * Activates any requested extensions that the agent supports.
         *
         * @param context the request context
         */
        private void handleExtensions(final RequestContext context) {
            Set<String> requestedUris = context.getCallContext().getRequestedExtensions();
            for (String uri : requestedUris) {
                if (supportedExtensionUris.contains(uri)) {
                    context.getCallContext().activateExtension(uri);
                    logger.info("Activated extension: " + uri);
                }
            }
        }

        /**
         * Parses the request message into text and data parts.
         *
         * @param message the incoming message
         * @param textParts list to populate with text parts
         * @param dataParts list to populate with data parts
         */
        private void parseRequest(final Message message,
                                  final List<TextPart> textParts,
                                  final List<DataPart> dataParts) {
            if (message == null || message.getParts() == null) {
                return;
            }

            for (Part<?> part : message.getParts()) {
                if (part instanceof TextPart textPart) {
                    textParts.add(textPart);
                } else if (part instanceof DataPart dataPart) {
                    dataParts.add(dataPart);
                }
            }
        }

        /**
         * Handles the request by using AI to determine which tool to use and executing it.
         *
         * This method uses LangChain4j AI-powered tool selection to intelligently
         * determine the appropriate operation based on the user's request and available
         * data parts. This mirrors the Python implementation's FunctionCallResolver.
         *
         * @param textParts the text parts from the request
         * @param dataParts the data parts from the request
         * @param updater the task updater
         * @param currentTask the current task
         * @throws AP2Exception if an error occurs during tool execution
         */
        private void handleRequest(final List<TextPart> textParts,
                                   final List<DataPart> dataParts,
                                   final TaskUpdater updater,
                                   final Task currentTask) throws AP2Exception {
            String prompt = !textParts.isEmpty() ? textParts.get(0).getText().strip() : "";

            logger.info("Processing request: " + prompt);

            // Use the agent to determine which tool to use
            // This mirrors the Python FunctionCallResolver.determine_tool_to_use()
            String toolName = agent.selectTool(prompt);
            logger.info("Agent selected tool: " + toolName);

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

        @Override
        public void cancel(final RequestContext context,
                           final EventQueue eventQueue) throws JSONRPCError {
            final Task task = context.getTask();

            if (task.getStatus().state() == TaskState.CANCELED) {
                // Task already cancelled
                throw new TaskNotCancelableError();
            }

            if (task.getStatus().state() == TaskState.COMPLETED) {
                // Task already completed
                throw new TaskNotCancelableError();
            }

            // Cancel the task
            final TaskUpdater updater = new TaskUpdater(context, eventQueue);
            updater.cancel();
        }
    }
}

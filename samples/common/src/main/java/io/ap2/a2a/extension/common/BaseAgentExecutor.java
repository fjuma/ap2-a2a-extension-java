package io.ap2.a2a.extension.common;

import static io.ap2.a2a.extension.spec.AP2Constants.PAYMENT_MANDATE_DATA_KEY;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.AgentExtension;
import io.a2a.spec.DataPart;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import io.ap2.a2a.extension.spec.AP2Exception;
import io.ap2.a2a.extension.spec.PaymentMandate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A baseline A2A AgentExecutor utilized by multiple agents.
 * This provides common functionality for A2A agents:
 * 1. It accepts a list of supported A2A extensions. Upon receiving a message, it
 *    activates any requested extensions that the agent supports.
 * 2. It leverages AI (via LangChain4j) to identify the appropriate tool to
 *    use for a given request, and invoking it to complete the task.
 * 3. It logs key events in the Agent Payments Protocol to the watch log.
 */
public abstract class BaseAgentExecutor implements AgentExecutor {

    private static final Logger logger = Logger.getLogger(BaseAgentExecutor.class.getName());

    private final Set<String> supportedExtensionUris;
    private final ToolSelectorAgent agent;

    /**
     * Constructor for BaseAgentExecutor.
     *
     * @param supportedExtensions the list of extensions this agent supports (from agent card)
     * @param agent the AI agent for tool selection (configured with system prompt)
     */
    protected BaseAgentExecutor(final List<AgentExtension> supportedExtensions,
                                final ToolSelectorAgent agent) {
        if (supportedExtensions != null) {
            this.supportedExtensionUris = supportedExtensions.stream()
                .map(AgentExtension::uri)
                .collect(Collectors.toSet());
        } else {
            this.supportedExtensionUris = Collections.emptySet();
        }
        this.agent = agent;
    }

    /**
     * Gets the supported extension URIs.
     *
     * @return the set of supported extension URIs
     */
    protected Set<String> getSupportedExtensionUris() {
        return supportedExtensionUris;
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
            // Log requested extensions to watch log
            WatchLog.logA2aRequestExtensions(context.getCallContext().getRequestedExtensions());

            // Parse the request
            List<TextPart> textParts = new ArrayList<>();
            List<DataPart> dataParts = new ArrayList<>();
            parseRequest(context.getMessage(), textParts, dataParts);

            // Log message parts to watch log
            List<String> textStrings = textParts.stream()
                .map(TextPart::getText)
                .toList();
            WatchLog.logA2aMessageParts(textStrings, dataParts);

            // Handle extension activation
            handleExtensions(context);

            // Verify that the AP2 extension is activated
            if (context.getCallContext().getActivatedExtensions().contains(A2aExtensionUtils.EXTENSION_URI)) {
                // Validate payment mandate signature if present
                Object paymentMandateData = MessageUtils.findDataPart(PAYMENT_MANDATE_DATA_KEY, dataParts);
                if (paymentMandateData != null) {
                    PaymentMandate paymentMandate = MessageUtils.parseCanonicalObject(
                        PAYMENT_MANDATE_DATA_KEY, dataParts, PaymentMandate.class);
                    Validation.validatePaymentMandateSignature(paymentMandate);
                }
            } else {
                throw new AP2Exception(
                    "Payment extension not activated: " + context.getCallContext().getActivatedExtensions());
            }

            logger.info("Server working on (context_id, task_id): ("
                + updater.getContextId() + ", " + updater.getTaskId() + ")");

            // Delegate to subclass for handling the request
            // This allows subclasses to add custom validation (like merchant's shopping agent validation)
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
     * This mirrors the Python _handle_extensions() method.
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
     * This mirrors the Python _parse_request() method.
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
     * Handles the request by analyzing the text and data parts.
     * Subclasses must implement this to provide their own logic for:
     * - Additional validation (e.g., shopping agent validation)
     * - AI-based tool selection
     * - Tool dispatch
     *
     * This mirrors the Python _handle_request() method.
     *
     * @param textParts the text parts from the request
     * @param dataParts the data parts from the request
     * @param updater the task updater
     * @param currentTask the current task
     * @throws AP2Exception if an error occurs during request handling
     */
    protected abstract void handleRequest(final List<TextPart> textParts,
                                          final List<DataPart> dataParts,
                                          final TaskUpdater updater,
                                          final Task currentTask) throws AP2Exception;

    /**
     * Selects the appropriate tool based on the user's request.
     * This mirrors the Python FunctionCallResolver.determine_tool_to_use().
     *
     * @param userPrompt the user's request
     * @return the name of the selected tool
     */
    protected String selectTool(final String userPrompt) {
        String toolName = agent.selectTool(userPrompt);
        logger.info("Agent selected tool: " + toolName);
        return toolName;
    }

    /**
     * A helper function to fail a task with a given error message.
     *
     * @param updater the TaskUpdater instance
     * @param errorText the error message
     */
    protected void failTask(final TaskUpdater updater, final String errorText) {
        try {
            Message errorMessage = updater.newAgentMessage(
                List.of(new TextPart(errorText)), null);
            updater.fail(errorMessage);
        } catch (Exception e) {
            logger.severe("Failed to fail task: " + e.getMessage());
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

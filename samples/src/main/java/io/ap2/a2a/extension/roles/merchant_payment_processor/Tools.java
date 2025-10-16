package io.ap2.a2a.extension.roles.merchant_payment_processor;

import static io.ap2.a2a.extension.spec.AP2Constants.EXTENSION_URI;
import static io.ap2.a2a.extension.spec.AP2Constants.PAYMENT_MANDATE_DATA_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Logger;

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DataPart;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import io.ap2.a2a.extension.common.A2aMessageBuilder;
import io.ap2.a2a.extension.common.MessageUtils;
import io.ap2.a2a.extension.spec.AP2Exception;
import io.ap2.a2a.extension.spec.PaymentMandate;

/**
 * Tools for the merchant payment processor agent.
 * <p>
 * Each agent uses individual tools to handle distinct tasks throughout the
 * shopping and purchasing process.
 */
public class Tools {

    private static final Logger logger = Logger.getLogger(Tools.class.getName());

    /**
     * Handles the initiation of a payment.
     *
     * @param dataParts The data parts from the request, expected to contain a PaymentMandate.
     * @param updater The TaskUpdater instance for updating the task state.
     * @param currentTask The current task, or null if this is a new payment.
     * @param clientFactory A function that creates an A2A client given a base URL and set of required extensions.
     * @param debugMode Whether the agent is in debug mode.
     * @throws AP2Exception if required data is missing or invalid
     */
    public void initiatePayment(
            List<DataPart> dataParts,
            TaskUpdater updater,
            Task currentTask,
            BiFunction<String, Set<String>, Client> clientFactory,
            boolean debugMode) throws AP2Exception {

        PaymentMandate paymentMandate = MessageUtils.parseCanonicalObject(
                PAYMENT_MANDATE_DATA_KEY, dataParts, PaymentMandate.class);
        if (paymentMandate == null) {
            Message errorMessage = updater.newAgentMessage(
                    createTextParts("Missing payment_mandate."), null);
            updater.fail(errorMessage);
            throw new AP2Exception("Missing payment_mandate.");
        }

        String challengeResponse = (String) MessageUtils.findDataPart("challenge_response", dataParts);
        if (challengeResponse == null) {
            challengeResponse = "";
        }

        handlePaymentMandate(paymentMandate, challengeResponse, updater, currentTask, clientFactory, debugMode);
    }

    /**
     * Handles a payment mandate.
     * <p>
     * If no task is present, it initiates a transaction challenge. If a task
     * requires input, it verifies the challenge response and completes the payment.
     *
     * @param paymentMandate The payment mandate containing payment details.
     * @param challengeResponse The response to a transaction challenge, if any.
     * @param updater The task updater for managing task state.
     * @param currentTask The current task, or null if it's a new payment.
     * @param clientFactory A function that creates an A2A client.
     * @param debugMode Whether the agent is in debug mode.
     * @throws AP2Exception if there's an error processing the payment
     */
    private void handlePaymentMandate(
            PaymentMandate paymentMandate,
            String challengeResponse,
            TaskUpdater updater,
            Task currentTask,
            BiFunction<String, Set<String>, Client> clientFactory,
            boolean debugMode) throws AP2Exception {

        if (currentTask == null) {
            raiseChallenge(updater);
            return;
        }

        if (currentTask.getStatus().state() == TaskState.INPUT_REQUIRED) {
            checkChallengeResponseAndCompletePayment(
                    paymentMandate, challengeResponse, updater, clientFactory, debugMode);
        }
    }

    /**
     * Raises a transaction challenge.
     * <p>
     * This challenge would normally be raised by the issuer, but we don't
     * have an issuer in the demo, so we raise the challenge here. For concreteness,
     * we are using an OTP challenge in this sample.
     *
     * @param updater The task updater.
     * @throws AP2Exception if there's an error raising the challenge
     */
    private void raiseChallenge(TaskUpdater updater) throws AP2Exception {
        Map<String, Object> challengeData = new HashMap<>();
        challengeData.put("type", "otp");
        challengeData.put("display_text",
                "The payment method issuer sent a verification code to the phone " +
                        "number on file, please enter it below. It will be shared with the " +
                        "issuer so they can authorize the transaction. " +
                        "(Demo only hint: the code is 123)");

        List<Part<?>> parts = new ArrayList<>();
        parts.add(new TextPart(
                "Please provide the challenge response to complete the payment."));
        parts.add(new DataPart(Map.of("challenge", challengeData)));

        Message message = updater.newAgentMessage(parts, null);
        updater.requiresInput(message);
    }

    /**
     * Checks the challenge response and completes the payment process.
     * <p>
     * Checking the challenge response would be done by the issuer, but we don't
     * have an issuer in the demo, so we do it here.
     *
     * @param paymentMandate The payment mandate.
     * @param challengeResponse The challenge response.
     * @param updater The task updater.
     * @param clientFactory A function that creates an A2A client.
     * @param debugMode Whether the agent is in debug mode.
     * @throws AP2Exception if there's an error processing the payment
     */
    private void checkChallengeResponseAndCompletePayment(
            PaymentMandate paymentMandate,
            String challengeResponse,
            TaskUpdater updater,
            BiFunction<String, Set<String>, Client> clientFactory,
            boolean debugMode) throws AP2Exception {

        if (challengeResponseIsValid(challengeResponse)) {
            completePayment(paymentMandate, updater, clientFactory, debugMode);
            return;
        }

        Message message = updater.newAgentMessage(
                createTextParts("Challenge response incorrect."), null);
        updater.requiresInput(message);
    }

    /**
     * Completes the payment process.
     *
     * @param paymentMandate The payment mandate.
     * @param updater The task updater.
     * @param clientFactory A function that creates an A2A client.
     * @param debugMode Whether the agent is in debug mode.
     * @throws AP2Exception if there's an error completing the payment
     */
    private void completePayment(
            PaymentMandate paymentMandate,
            TaskUpdater updater,
            BiFunction<String, Set<String>, Client> clientFactory,
            boolean debugMode) throws AP2Exception {

        String paymentMandateId = paymentMandate.paymentMandateContents().paymentMandateId();
        String paymentCredential = requestPaymentCredential(paymentMandate, updater, clientFactory, debugMode);

        logger.info("Calling issuer to complete payment for " + paymentMandateId +
                " with payment credential " + paymentCredential + "...");

        // Call issuer to complete the payment
        Message successMessage = updater.newAgentMessage(
                createTextParts("{'status': 'success'}"), null);
        updater.complete(successMessage);
    }

    /**
     * Validates the challenge response.
     *
     * @param challengeResponse The challenge response to validate.
     * @return true if the challenge response is valid, false otherwise.
     */
    private boolean challengeResponseIsValid(String challengeResponse) {
        return "123".equals(challengeResponse);
    }

    /**
     * Sends a request to the Credentials Provider for payment credentials.
     *
     * @param paymentMandate The PaymentMandate containing payment details.
     * @param updater The task updater.
     * @param clientFactory A function that creates an A2A client.
     * @param debugMode Whether the agent is in debug mode.
     * @return The payment credential details.
     * @throws AP2Exception if the payment method data cannot be found
     */
    private String requestPaymentCredential(
            PaymentMandate paymentMandate,
            TaskUpdater updater,
            BiFunction<String, Set<String>, Client> clientFactory,
            boolean debugMode) throws AP2Exception {

        Map<String, Object> details = paymentMandate.paymentMandateContents()
                .paymentResponse().details();

        @SuppressWarnings("unchecked")
        Map<String, Object> tokenObject = (Map<String, Object>) details.get("token");
        if (tokenObject == null) {
            throw new AP2Exception("Token object not found in payment response details");
        }

        String credentialsProviderUrl = (String) tokenObject.get("url");
        if (credentialsProviderUrl == null) {
            throw new AP2Exception("Credentials provider URL not found in token");
        }

        Client credentialsProvider = clientFactory.apply(credentialsProviderUrl, Set.of(EXTENSION_URI));

        A2aMessageBuilder messageBuilder = new A2aMessageBuilder()
                .setContextId(updater.getContextId())
                .addText("Give me the payment method credentials for the given token.")
                .addData(PAYMENT_MANDATE_DATA_KEY, paymentMandate)
                .addData("debug_mode", debugMode);

        // Container to hold the payment credential result
        String[] paymentCredentialHolder = new String[1];
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();

        consumers.add((event, agentCard) -> {
            if (event instanceof TaskEvent taskEvent) {
                if (taskEvent.getTask().getArtifacts() != null) {
                    Task task = taskEvent.getTask();
                    if (!task.getArtifacts().isEmpty()) {
                        // Get the first data part from artifacts
                        Object firstDataPart = task.getArtifacts().get(0);
                        if (firstDataPart instanceof DataPart) {
                            DataPart dataPart = (DataPart) firstDataPart;
                            // Convert the data part to string representation
                            paymentCredentialHolder[0] = dataPart.getData().toString();
                        }
                    }
                }
            }
        });

        Consumer<Throwable> errorHandler = throwable -> {
            logger.severe("Error requesting payment credential: " + throwable.getMessage());
        };

        try {
            credentialsProvider.sendMessage(messageBuilder.build(), consumers, errorHandler, null);
        } catch (Exception e) {
            throw new AP2Exception("Failed to request payment credential: " + e.getMessage(), e);
        }

        if (paymentCredentialHolder[0] == null) {
            throw new AP2Exception("Failed to find the payment method data.");
        }

        return paymentCredentialHolder[0];
    }

    /**
     * Helper to create text parts.
     *
     * @param texts The text strings to convert to parts.
     * @return A list of TextPart objects.
     */
    private List<Part<?>> createTextParts(String... texts) {
        List<Part<?>> parts = new ArrayList<>();
        for (String text : texts) {
            parts.add(new TextPart(text));
        }
        return parts;
    }
}

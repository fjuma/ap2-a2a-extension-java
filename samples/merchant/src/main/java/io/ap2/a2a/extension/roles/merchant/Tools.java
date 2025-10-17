package io.ap2.a2a.extension.roles.merchant;

import static io.ap2.a2a.extension.spec.AP2Constants.CART_MANDATE_DATA_KEY;
import static io.ap2.a2a.extension.spec.AP2Constants.EXTENSION_URI;
import static io.ap2.a2a.extension.spec.AP2Constants.PAYMENT_MANDATE_DATA_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
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
import io.a2a.spec.TextPart;
import io.ap2.a2a.extension.common.A2aMessageBuilder;
import io.ap2.a2a.extension.common.MessageUtils;
import io.ap2.a2a.extension.common.PaymentRemoteA2aClient;
import io.ap2.a2a.extension.spec.AP2Exception;
import io.ap2.a2a.extension.spec.CartContents;
import io.ap2.a2a.extension.spec.CartMandate;
import io.ap2.a2a.extension.spec.ContactAddress;
import io.ap2.a2a.extension.spec.PaymentCurrencyAmount;
import io.ap2.a2a.extension.spec.PaymentDetailsInit;
import io.ap2.a2a.extension.spec.PaymentItem;
import io.ap2.a2a.extension.spec.PaymentMandate;
import io.ap2.a2a.extension.spec.PaymentRequest;

/**
 * Tools used by the merchant agent.
 * <p>
 * Each agent uses individual tools to handle distinct tasks throughout the
 * shopping and purchasing process.
 */
public class Tools {

    private static final Logger logger = Logger.getLogger(Tools.class.getName());

    /**
     * A map of payment method types to their corresponding processor agent URLs.
     * This is the set of linked Merchant Payment Processor Agents this Merchant
     * is integrated with.
     */
    private static final Map<String, String> PAYMENT_PROCESSORS_BY_PAYMENT_METHOD_TYPE = Map.of(
            "CARD", "http://localhost:8003/a2a/merchant_payment_processor_agent"
    );

    /**
     * A placeholder for a JSON Web Token (JWT) used for merchant authorization.
     */
    private static final String FAKE_JWT = "eyJhbGciOiJSUzI1NiIsImtpZIwMjQwOTA...";

    /**
     * Updates an existing cart after a shipping address is provided.
     *
     * @param dataParts A list of data part contents from the request.
     * @param updater The TaskUpdater instance to add artifacts and complete the task.
     * @param currentTask The current task -- not used in this function.
     * @param cartMandateStore The cart mandate store for retrieving and storing cart mandates.
     * @param debugMode Whether the agent is in debug mode.
     * @throws AP2Exception if required data is missing or invalid
     */
    public void updateCart(
            List<DataPart> dataParts,
            TaskUpdater updater,
            Task currentTask,
            CartMandateStore cartMandateStore,
            boolean debugMode) throws AP2Exception {

        String cartId = (String) MessageUtils.findDataPart("cart_id", dataParts);
        if (cartId == null) {
            failTask(updater, "Missing cart_id.");
            return;
        }

        Map<String, Object> shippingAddressMap =
                (Map<String, Object>) MessageUtils.findDataPart("shipping_address", dataParts);
        if (shippingAddressMap == null) {
            failTask(updater, "Missing shipping_address.");
            return;
        }

        CartMandate cartMandate = cartMandateStore.getCartMandate(cartId);
        if (cartMandate == null) {
            failTask(updater, "CartMandate not found for cart_id: " + cartId);
            return;
        }

        String riskData = cartMandateStore.getRiskData(updater.getContextId());
        if (riskData == null) {
            failTask(updater, "Missing risk_data for context_id: " + updater.getContextId());
            return;
        }

        // Update the CartMandate with new shipping and tax cost.
        try {
            // Convert shipping address map to ContactAddress
            ContactAddress shippingAddress = convertToContactAddress(shippingAddressMap);

            // Create new CartContents with updated shipping address
            CartContents oldContents = cartMandate.contents();
            PaymentRequest oldPaymentRequest = oldContents.paymentRequest();

            PaymentRequest updatedPaymentRequest = new PaymentRequest(
                    oldPaymentRequest.methodData(),
                    oldPaymentRequest.details(),
                    oldPaymentRequest.options(),
                    shippingAddress
            );

            // Add new shipping and tax costs to the PaymentRequest (hardcoded as in Python)
            List<PaymentItem> taxAndShippingCosts = List.of(
                    new PaymentItem(
                            "Shipping",
                            new PaymentCurrencyAmount("USD", 2.00),
                            null,
                            null
                    ),
                    new PaymentItem(
                            "Tax",
                            new PaymentCurrencyAmount("USD", 1.50),
                            null,
                            null
                    )
            );

            PaymentDetailsInit oldDetails = updatedPaymentRequest.details();
            List<PaymentItem> displayItems = new ArrayList<>(oldDetails.displayItems());
            displayItems.addAll(taxAndShippingCosts);

            // Recompute the total amount
            double totalAmount = displayItems.stream()
                    .mapToDouble(item -> item.amount().value())
                    .sum();

            PaymentItem updatedTotal = new PaymentItem(
                    oldDetails.total().label(),
                    new PaymentCurrencyAmount(
                            oldDetails.total().amount().currency(),
                            totalAmount
                    ),
                    oldDetails.total().pending(),
                    oldDetails.total().refundPeriod()
            );

            PaymentDetailsInit updatedDetails = new PaymentDetailsInit(
                    oldDetails.id(),
                    displayItems,
                    oldDetails.shippingOptions(),
                    oldDetails.modifiers(),
                    updatedTotal
            );

            PaymentRequest finalPaymentRequest = new PaymentRequest(
                    updatedPaymentRequest.methodData(),
                    updatedDetails,
                    updatedPaymentRequest.options(),
                    updatedPaymentRequest.shippingAddress()
            );

            CartContents updatedContents = new CartContents(
                    oldContents.id(),
                    oldContents.userCartConfirmationRequired(),
                    finalPaymentRequest,
                    oldContents.cartExpiry(),
                    oldContents.merchantName()
            );

            // Create updated CartMandate with merchant authorization
            CartMandate updatedCartMandate = new CartMandate(updatedContents, FAKE_JWT);

            // Store the updated cart mandate
            cartMandateStore.setCartMandate(cartId, updatedCartMandate);

            // Add artifacts and complete
            updater.addArtifact(
                    List.of(
                            new DataPart(Map.of(CART_MANDATE_DATA_KEY, updatedCartMandate)),
                            new DataPart(Map.of("risk_data", riskData))));
            updater.complete();

        } catch (Exception e) {
            failTask(updater, "Invalid CartMandate after update: " + e.getMessage());
        }
    }

    /**
     * Initiates a payment for a given payment mandate. Use to make a payment.
     *
     * @param dataParts The data parts from the request, expected to contain a
     *                  PaymentMandate and optionally a challenge response.
     * @param updater The TaskUpdater instance for updating the task state.
     * @param currentTask The current task, used to find the processor's task ID.
     * @param cartMandateStore The cart mandate store for storing risk data.
     * @param debugMode Whether the agent is in debug mode.
     * @throws AP2Exception if required data is missing
     */
    public void initiatePayment(
            List<DataPart> dataParts,
            TaskUpdater updater,
            Task currentTask,
            CartMandateStore cartMandateStore,
            boolean debugMode) throws AP2Exception {

        PaymentMandate paymentMandate = MessageUtils.parseCanonicalObject(
                PAYMENT_MANDATE_DATA_KEY, dataParts, PaymentMandate.class);
        if (paymentMandate == null) {
            failTask(updater, "Missing payment_mandate.");
            return;
        }

        String riskData = (String) MessageUtils.findDataPart("risk_data", dataParts);
        if (riskData == null) {
            failTask(updater, "Missing risk_data.");
            return;
        }

        String paymentMethodType = paymentMandate.paymentMandateContents()
                .paymentResponse().methodName();
        String processorUrl = PAYMENT_PROCESSORS_BY_PAYMENT_METHOD_TYPE.get(paymentMethodType);

        if (processorUrl == null) {
            failTask(updater, "No payment processor found for method: " + paymentMethodType);
            return;
        }

        // Create the payment processor client using PaymentRemoteA2aClient
        PaymentRemoteA2aClient remoteClient = new PaymentRemoteA2aClient(
                "merchant_payment_processor",
                processorUrl,
                Set.of(EXTENSION_URI)
        );

        Client paymentProcessorAgent;
        try {
            paymentProcessorAgent = remoteClient.getA2aClient(List.of());
        } catch (Exception e) {
            failTask(updater, "Failed to create payment processor client: " + e.getMessage());
            return;
        }

        // Build the message to send to the payment processor
        A2aMessageBuilder messageBuilder = new A2aMessageBuilder()
                .setContextId(updater.getContextId())
                .addText("initiate_payment")
                .addData(PAYMENT_MANDATE_DATA_KEY, paymentMandate)
                .addData("risk_data", riskData)
                .addData("debug_mode", debugMode);

        // Add challenge response if present
        String challengeResponse = (String) MessageUtils.findDataPart("challenge_response", dataParts);
        if (challengeResponse != null && !challengeResponse.isEmpty()) {
            messageBuilder.addData("challenge_response", challengeResponse);
        }

        // Get the payment processor task ID if it exists
        String paymentProcessorTaskId = getPaymentProcessorTaskId(currentTask);
        if (paymentProcessorTaskId != null) {
            messageBuilder.setTaskId(paymentProcessorTaskId);
        }

        // Send the message and update the task status
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();

        // Add consumer to handle task status updates
        consumers.add((event, agentCard) -> {
            if (event instanceof TaskEvent taskEvent) {
                Task task = taskEvent.getTask();
                try {
                    updater.updateStatus(task.getStatus().state(), task.getStatus().message());
                } catch (Exception e) {
                    logger.severe("Failed to update task status: " + e.getMessage());
                }
            }
        });

        // Error handler for streaming errors
        Consumer<Throwable> errorHandler = throwable -> {
            logger.severe("Error during payment processor communication: " + throwable.getMessage());
        };

        try {
            paymentProcessorAgent.sendMessage(messageBuilder.build(), consumers, errorHandler, null);
        } catch (Exception e) {
            logger.severe("Failed to send message to payment processor: " + e.getMessage());
            throw new AP2Exception("Failed to initiate payment: " + e.getMessage(), e);
        }
    }

    /**
     * Receives and validates a DPC response to finalize payment.
     * <p>
     * This tool receives the Digital Payment Credential (DPC) response, in the form
     * of an OpenID4VP JSON, validates it, and simulates payment finalization.
     *
     * @param dataParts A list of data part contents from the request.
     * @param updater The TaskUpdater instance to add artifacts and complete the task.
     * @param currentTask The current task, not used in this function.
     * @throws AP2Exception if required data is missing
     */
    public void dpcFinish(
            List<DataPart> dataParts,
            TaskUpdater updater,
            Task currentTask) throws AP2Exception {

        String dpcResponse = (String) MessageUtils.findDataPart("dpc_response", dataParts);
        if (dpcResponse == null) {
            failTask(updater, "Missing dpc_response.");
            return;
        }

        logger.info("Received DPC response for finalization: " + dpcResponse);

        // --- Sample validation and payment finalization logic ---
        // TODO: Validate the nonce, and other merchant-specific attributes from the
        // DPC response.
        // TODO: Pass the DPC response to the payment processor agent for validation.

        // Simulate payment finalization.
        Map<String, Object> paymentResult = new HashMap<>();
        paymentResult.put("payment_status", "SUCCESS");
        paymentResult.put("transaction_id", "txn_1234567890");

        updater.addArtifact(
                List.of(new DataPart(paymentResult)));
        updater.complete();
    }

    /**
     * Returns the task ID of the payment processor task, if it exists.
     * <p>
     * Identified by assuming the first message with a task ID that is not the
     * merchant's task ID is a payment processor message.
     *
     * @param task The current task
     * @return The payment processor task ID, or null if not found
     */
    private String getPaymentProcessorTaskId(Task task) {
        if (task == null) {
            return null;
        }
        for (Message message : task.getHistory()) {
            if (!message.getTaskId().equals(task.getId())) {
                return message.getTaskId();
            }
        }
        return null;
    }

    /**
     * A helper function to fail a task with a given error message.
     *
     * @param updater The TaskUpdater instance
     * @param errorText The error message
     * @throws AP2Exception always
     */
    private void failTask(TaskUpdater updater, String errorText) throws AP2Exception {
        Message errorMessage = updater.newAgentMessage(
                List.of(new TextPart(errorText)), null);
        updater.fail(errorMessage);
        throw new AP2Exception(errorText);
    }

    /**
     * Converts a map representation of a ContactAddress to a ContactAddress object.
     *
     * @param addressMap The map containing contact address fields
     * @return The ContactAddress object
     */
    private ContactAddress convertToContactAddress(Map<String, Object> addressMap) {
        return new ContactAddress(
                (String) addressMap.get("city"),
                (String) addressMap.get("country"),
                (String) addressMap.get("dependent_locality"),
                (String) addressMap.get("organization"),
                (String) addressMap.get("phone_number"),
                (String) addressMap.get("postal_code"),
                (String) addressMap.get("recipient"),
                (String) addressMap.get("region"),
                (String) addressMap.get("sorting_code"),
                (List<String>) addressMap.get("address_line")
        );
    }
}

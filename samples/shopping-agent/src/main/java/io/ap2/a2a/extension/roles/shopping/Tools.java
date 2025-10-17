package io.ap2.a2a.extension.roles.shopping;

import static io.ap2.a2a.extension.spec.AP2Constants.CART_MANDATE_DATA_KEY;
import static io.ap2.a2a.extension.spec.AP2Constants.PAYMENT_MANDATE_DATA_KEY;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
import io.ap2.a2a.extension.spec.ContactAddress;
import io.ap2.a2a.extension.spec.PaymentMandate;
import io.ap2.a2a.extension.spec.PaymentMandateContents;
import io.ap2.a2a.extension.spec.PaymentResponse;
import io.ap2.a2a.extension.roles.shopping.subagents.payment.method.collector.PaymentMethodCollectorAgent;
import io.ap2.a2a.extension.roles.shopping.subagents.shipping.address.collector.ShippingAddressCollectorAgent;
import io.ap2.a2a.extension.roles.shopping.subagents.shopper.ShopperAgent;

/**
 * Tools used by the Shopping Agent.
 * <p>
 * Each agent uses individual tools to handle distinct tasks throughout the
 * shopping and purchasing process, such as updating a cart or initiating payment.
 */
@ApplicationScoped
public class Tools {

    private static final Logger logger = Logger.getLogger(Tools.class.getName());

    @Inject
    ShopperAgent shopperAgent;

    @Inject
    ShippingAddressCollectorAgent shippingAddressCollectorAgent;

    @Inject
    PaymentMethodCollectorAgent paymentMethodCollectorAgent;

    private Map<String, Object> state;
    private Client merchantClient;
    private Client credentialsProviderClient;
    private boolean debugMode;

    /**
     * Initializes the tools with required context.
     * Must be called before tools can be invoked by the AI agent.
     *
     * @param state the shared state map
     * @param debugMode whether debug mode is enabled
     */
    public void initialize(Map<String, Object> state, boolean debugMode) {
        this.state = state;
        this.debugMode = debugMode;
    }

    /**
     * Gets or creates the merchant client from the registry.
     *
     * @return the merchant agent client
     */
    private Client getMerchantClient() {
        if (merchantClient == null) {
            try {
                merchantClient = RemoteClientRegistry.MERCHANT_AGENT_CLIENT.getA2aClient(List.of());
            } catch (Exception e) {
                throw new RuntimeException("Failed to create merchant client", e);
            }
        }
        return merchantClient;
    }

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
     * Delegates to the shopper agent to help the user shop for products.
     *
     * @param assignment the shopping task assignment
     * @return the result from the shopper agent
     */
    public String shopper(String assignment) {
        return shopperAgent.shopForProducts(assignment);
    }

    /**
     * Delegates to the shipping address collector agent to collect the user's shipping address.
     *
     * @param assignment the shipping address collection task assignment
     * @return the shipping address from the agent
     */
    public String shippingAddressCollector(String assignment) {
        return shippingAddressCollectorAgent.collectShippingAddress(assignment);
    }

    /**
     * Delegates to the payment method collector agent to collect the user's payment method.
     *
     * @param assignment the payment method collection task assignment
     * @return the payment method alias from the agent
     */
    public String paymentMethodCollector(String assignment) {
        return paymentMethodCollectorAgent.collectPaymentMethod(assignment);
    }

    /**
     * Notifies the merchant agent of a shipping address selection for a cart.
     *
     * @param shippingAddress The user's selected shipping address.
     * @return The updated CartMandate.
     * @throws AP2Exception if required state is missing or operation fails
     */
    @Tool("Update the cart with the user's shipping address")
    public CartMandate updateCart(ContactAddress shippingAddress) throws AP2Exception {

        String chosenCartId = (String) state.get("chosen_cart_id");
        if (chosenCartId == null) {
            throw new AP2Exception("No chosen cart mandate found in tool context state.");
        }

        String shoppingContextId = (String) state.get("shopping_context_id");

        A2aMessageBuilder messageBuilder = new A2aMessageBuilder()
                .setContextId(shoppingContextId)
                .addText("Update the cart with the user's shipping address.")
                .addData("cart_id", chosenCartId)
                .addData("shipping_address", shippingAddress)
                .addData("shopping_agent_id", "trusted_shopping_agent")
                .addData("debug_mode", debugMode);

        CartMandate[] updatedCartMandateHolder = new CartMandate[1];
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();

        consumers.add((event, agentCard) -> {
            if (event instanceof TaskEvent taskEvent) {
                Task task = taskEvent.getTask();
                if (task.getArtifacts() != null && !task.getArtifacts().isEmpty()) {
                    List<CartMandate> cartMandates = parseCartMandates(task.getArtifacts());
                    if (!cartMandates.isEmpty()) {
                        updatedCartMandateHolder[0] = cartMandates.get(0);
                    }
                }
            }
        });

        Consumer<Throwable> errorHandler = throwable -> {
            logger.severe("Error updating cart: " + throwable.getMessage());
        };

        try {
            getMerchantClient().sendMessage(messageBuilder.build(), consumers, errorHandler, null);
        } catch (Exception e) {
            throw new AP2Exception("Failed to update cart: " + e.getMessage(), e);
        }

        if (updatedCartMandateHolder[0] == null) {
            throw new AP2Exception("Failed to get updated cart mandate.");
        }

        CartMandate updatedCartMandate = updatedCartMandateHolder[0];
        state.put("cart_mandate", updatedCartMandate);
        state.put("shipping_address", shippingAddress);

        return updatedCartMandate;
    }

    /**
     * Initiates a payment using the payment mandate from state.
     *
     * @return The status of the payment initiation.
     * @throws AP2Exception if required state is missing or operation fails
     */
    @Tool("Initiate a payment using the signed payment mandate")
    public TaskStatus initiatePayment() throws AP2Exception {

        PaymentMandate paymentMandate = (PaymentMandate) state.get("signed_payment_mandate");
        if (paymentMandate == null) {
            throw new AP2Exception("No signed payment mandate found in tool context state.");
        }

        String riskData = (String) state.get("risk_data");
        if (riskData == null) {
            throw new AP2Exception("No risk data found in tool context state.");
        }

        String shoppingContextId = (String) state.get("shopping_context_id");

        A2aMessageBuilder messageBuilder = new A2aMessageBuilder()
                .setContextId(shoppingContextId)
                .addText("Initiate a payment")
                .addData(PAYMENT_MANDATE_DATA_KEY, paymentMandate)
                .addData("risk_data", riskData)
                .addData("shopping_agent_id", "trusted_shopping_agent")
                .addData("debug_mode", debugMode);

        TaskStatus[] statusHolder = new TaskStatus[1];
        String[] taskIdHolder = new String[1];
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();

        consumers.add((event, agentCard) -> {
            if (event instanceof TaskEvent taskEvent) {
                Task task = taskEvent.getTask();
                statusHolder[0] = task.getStatus();
                taskIdHolder[0] = task.getId();
            }
        });

        Consumer<Throwable> errorHandler = throwable -> {
            logger.severe("Error initiating payment: " + throwable.getMessage());
        };

        try {
            getMerchantClient().sendMessage(messageBuilder.build(), consumers, errorHandler, null);
        } catch (Exception e) {
            throw new AP2Exception("Failed to initiate payment: " + e.getMessage(), e);
        }

        if (taskIdHolder[0] != null) {
            state.put("initiate_payment_task_id", taskIdHolder[0]);
        }

        return statusHolder[0];
    }

    /**
     * Initiates a payment using the payment mandate from state and a challenge response.
     * <p>
     * In our sample, the challenge response is a one-time password (OTP) sent to the user.
     *
     * @param challengeResponse The challenge response.
     * @return The status of the payment initiation.
     * @throws AP2Exception if required state is missing or operation fails
     */
    @Tool("Initiate a payment with the OTP challenge response")
    public TaskStatus initiatePaymentWithOtp(String challengeResponse) throws AP2Exception {

        PaymentMandate paymentMandate = (PaymentMandate) state.get("signed_payment_mandate");
        if (paymentMandate == null) {
            throw new AP2Exception("No signed payment mandate found in tool context state.");
        }

        String riskData = (String) state.get("risk_data");
        if (riskData == null) {
            throw new AP2Exception("No risk data found in tool context state.");
        }

        String shoppingContextId = (String) state.get("shopping_context_id");
        String initiatePaymentTaskId = (String) state.get("initiate_payment_task_id");

        A2aMessageBuilder messageBuilder = new A2aMessageBuilder()
                .setContextId(shoppingContextId)
                .setTaskId(initiatePaymentTaskId)
                .addText("Initiate a payment. Include the challenge response.")
                .addData(PAYMENT_MANDATE_DATA_KEY, paymentMandate)
                .addData("shopping_agent_id", "trusted_shopping_agent")
                .addData("challenge_response", challengeResponse)
                .addData("risk_data", riskData)
                .addData("debug_mode", debugMode);

        TaskStatus[] statusHolder = new TaskStatus[1];
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();

        consumers.add((event, agentCard) -> {
            if (event instanceof TaskEvent taskEvent) {
                Task task = taskEvent.getTask();
                statusHolder[0] = task.getStatus();
            }
        });

        Consumer<Throwable> errorHandler = throwable -> {
            logger.severe("Error initiating payment with OTP: " + throwable.getMessage());
        };

        try {
            getMerchantClient().sendMessage(messageBuilder.build(), consumers, errorHandler, null);
        } catch (Exception e) {
            throw new AP2Exception("Failed to initiate payment with OTP: " + e.getMessage(), e);
        }

        return statusHolder[0];
    }

    /**
     * Creates a payment mandate and stores it in state.
     *
     * @param paymentMethodAlias The payment method alias.
     * @param userEmail The user's email address.
     * @return The payment mandate.
     * @throws AP2Exception if required state is missing
     */
    @Tool("Create a payment mandate with the user's payment method and email")
    public PaymentMandate createPaymentMandate(
            String paymentMethodAlias,
            String userEmail) throws AP2Exception {

        CartMandate cartMandate = (CartMandate) state.get("cart_mandate");
        if (cartMandate == null) {
            throw new AP2Exception("No cart mandate found in tool context state.");
        }

        ContactAddress shippingAddress = (ContactAddress) state.get("shipping_address");
        Map<String, Object> paymentCredentialToken = (Map<String, Object>) state.get("payment_credential_token");

        PaymentResponse paymentResponse = new PaymentResponse(
                cartMandate.contents().paymentRequest().details().id(),
                "CARD",
                Map.of("token", paymentCredentialToken),
                shippingAddress,
                null,
                null,
                userEmail,
                null
        );

        PaymentMandateContents paymentMandateContents = new PaymentMandateContents(
                UUID.randomUUID().toString(),
                cartMandate.contents().paymentRequest().details().id(),
                cartMandate.contents().paymentRequest().details().total(),
                paymentResponse,
                cartMandate.contents().merchantName(),
                Instant.now().toString()
        );

        PaymentMandate paymentMandate = new PaymentMandate(paymentMandateContents, null);

        state.put("payment_mandate", paymentMandate);
        return paymentMandate;
    }

    /**
     * Simulates signing the transaction details on a user's secure device.
     * <p>
     * This function represents the step where the final transaction details,
     * including hashes of the cart and payment mandates, would be sent to a
     * secure hardware element on the user's device (e.g., Secure Enclave) to be
     * cryptographically signed with the user's private key.
     * <p>
     * Note: This is a placeholder implementation. It does not perform any actual
     * cryptographic operations. It simulates the creation of a signature by
     * concatenating the mandate hashes.
     *
     * @return A string representing the simulated user authorization signature (JWT).
     * @throws AP2Exception if required state is missing
     */
    @Tool("Sign the payment and cart mandates on the user's secure device")
    public String signMandatesOnUserDevice() throws AP2Exception {
        PaymentMandate paymentMandate = (PaymentMandate) state.get("payment_mandate");
        if (paymentMandate == null) {
            throw new AP2Exception("No payment mandate found in tool context state.");
        }

        CartMandate cartMandate = (CartMandate) state.get("cart_mandate");
        if (cartMandate == null) {
            throw new AP2Exception("No cart mandate found in tool context state.");
        }

        String cartMandateHash = generateCartMandateHash(cartMandate);
        String paymentMandateHash = generatePaymentMandateHash(paymentMandate.paymentMandateContents());

        // A JWT containing the user's digital signature to authorize the transaction.
        // The payload uses hashes to bind the signature to the specific cart and
        // payment details, and includes a nonce to prevent replay attacks.
        String userAuthorization = cartMandateHash + "_" + paymentMandateHash;

        PaymentMandate signedPaymentMandate = new PaymentMandate(
                paymentMandate.paymentMandateContents(),
                userAuthorization
        );

        state.put("signed_payment_mandate", signedPaymentMandate);
        return userAuthorization;
    }

    /**
     * Sends the signed payment mandate to the credentials provider.
     *
     * @return The task from the credentials provider.
     * @throws AP2Exception if required state is missing or operation fails
     */
    @Tool("Send the signed payment mandate to the credentials provider")
    public Task sendSignedPaymentMandateToCredentialsProvider() throws AP2Exception {

        PaymentMandate paymentMandate = (PaymentMandate) state.get("signed_payment_mandate");
        if (paymentMandate == null) {
            throw new AP2Exception("No signed payment mandate found in tool context state.");
        }

        String riskData = (String) state.get("risk_data");
        if (riskData == null) {
            throw new AP2Exception("No risk data found in tool context state.");
        }

        String shoppingContextId = (String) state.get("shopping_context_id");

        A2aMessageBuilder messageBuilder = new A2aMessageBuilder()
                .setContextId(shoppingContextId)
                .addText("This is the signed payment mandate")
                .addData(PAYMENT_MANDATE_DATA_KEY, paymentMandate)
                .addData("risk_data", riskData)
                .addData("debug_mode", debugMode);

        Task[] taskHolder = new Task[1];
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();

        consumers.add((event, agentCard) -> {
            if (event instanceof TaskEvent taskEvent) {
                taskHolder[0] = taskEvent.getTask();
            }
        });

        Consumer<Throwable> errorHandler = throwable -> {
            logger.severe("Error sending signed payment mandate: " + throwable.getMessage());
        };

        try {
            getCredentialsProviderClient().sendMessage(messageBuilder.build(), consumers, errorHandler, null);
        } catch (Exception e) {
            throw new AP2Exception("Failed to send signed payment mandate: " + e.getMessage(), e);
        }

        return taskHolder[0];
    }

    /**
     * Generates a cryptographic hash of the CartMandate.
     * <p>
     * This hash serves as a tamper-proof reference to the specific merchant-signed
     * cart offer that the user has approved.
     * <p>
     * Note: This is a placeholder implementation for development. A real
     * implementation must use a secure hashing algorithm (e.g., SHA-256) on the
     * canonical representation of the CartMandate object.
     *
     * @param cartMandate The complete CartMandate object, including the merchant's authorization.
     * @return A string representing the hash of the cart mandate.
     */
    private String generateCartMandateHash(CartMandate cartMandate) {
        return "fake_cart_mandate_hash_" + cartMandate.contents().id();
    }

    /**
     * Generates a cryptographic hash of the PaymentMandateContents.
     * <p>
     * This hash creates a tamper-proof reference to the specific payment details
     * the user is about to authorize.
     * <p>
     * Note: This is a placeholder implementation for development. A real
     * implementation must use a secure hashing algorithm (e.g., SHA-256) on the
     * canonical representation of the PaymentMandateContents object.
     *
     * @param paymentMandateContents The payment mandate contents to hash.
     * @return A string representing the hash of the payment mandate contents.
     */
    private String generatePaymentMandateHash(PaymentMandateContents paymentMandateContents) {
        return "fake_payment_mandate_hash_" + paymentMandateContents.paymentMandateId();
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
}

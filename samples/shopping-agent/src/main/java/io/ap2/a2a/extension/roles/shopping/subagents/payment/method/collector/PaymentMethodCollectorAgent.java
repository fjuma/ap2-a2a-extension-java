package io.ap2.a2a.extension.roles.shopping.subagents.payment.method.collector;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.ap2.a2a.extension.common.SystemUtils;
import io.quarkiverse.langchain4j.RegisterAiService;

/**
 * An agent responsible for collecting the user's choice of payment method.
 * <p>
 * The shopping agent delegates responsibility for collecting the user's choice of
 * payment method to this subagent, after the user has finalized their cart.
 * <p>
 * Through the getPaymentMethods and getPaymentCredentialToken tools,
 * the agent retrieves eligible payment methods from the credentials provider agent,
 * presents them to the user, and obtains a payment credential token.
 * <p>
 * This agent uses LangChain4j's tool calling capabilities to autonomously invoke
 * the registered tools during execution.
 */
@RegisterAiService(tools = Tools.class)
public interface PaymentMethodCollectorAgent {

    /**
     * Collects the user's payment method choice.
     *
     * @param assignment the task assignment for payment method collection
     * @return the payment method alias chosen by the user
     */
    @SystemMessage("""
        You are an agent responsible for obtaining the user's payment method for a
        purchase.

        """ + SystemUtils.DEBUG_MODE_INSTRUCTIONS + """

        When asked to complete a task, follow these instructions:
        1. Ensure a CartMandate object was provided to you.
        2. Present a clear and organized summary of the cart to the user. The
           summary should be divided into two main sections:
           a. Order Summary:
              Merchant: The name of the merchant.
              Item: Display the item_name clearly.
              Price Breakdown:
                Shipping: The shipping cost from the `shippingOptions`.
                Tax: The tax amount, if available.
                Total: The final total price from the `total` field in the
                  `payment_request`.
                Format all amounts with commas and the currency symbol.
              Expires: Convert the cart_expiry into a human-readable format
                (e.g., "in 2 hours," "by tomorrow at 5 PM"). Convert the time to the
                user's timezone.
              Refund Period: Convert the refund_period into a human-readable format
                (e.g., "30 days," "14 days").
           b. Show the full shipping address collected earlier in a well-formatted
              manner.
           Ensure the entire presentation is well-formatted and easy to read.
        3. Call the `getPaymentMethods` tool to get eligible
           payment_method_aliases with the method_data from the CartMandate's
           payment_request. Present the payment_method_aliases to the user in
           a numbered list.
        4. Ask the user to choose which of their forms of payment they would
           like to use for the payment. Remember that payment_method_alias.
        5. Call the `getPaymentCredentialToken` tool to get the payment
           credential token with the user_email and payment_method_alias.
        6. Transfer back to the root_agent with the payment_method_alias.
        """)
    @UserMessage("{{assignment}}")
    String collectPaymentMethod(String assignment);
}

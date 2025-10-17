package io.ap2.a2a.extension.roles.merchant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.ap2.a2a.extension.common.ToolSelectorAgent;
import io.ap2.a2a.extension.common.SystemUtils;
import io.quarkiverse.langchain4j.RegisterAiService;

/**
 * LangChain4j agent for merchant tool selection.
 * This agent uses an LLM to intelligently select the appropriate tool based on the user's request.
 */
@RegisterAiService
public interface MerchantAgent extends ToolSelectorAgent {

    /**
     * System message that guides the AI in selecting the appropriate tool.
     * This is similar to the _system_prompt in the Python implementation.
     */
    @SystemMessage("""
        You are a merchant agent. Your role is to help users with their shopping
        requests.

        You can find items, update shopping carts, and initiate payments.

        Based on the user's request, identify their intent and select the
        single correct tool to use. Your only output should be a tool call.
        Do not engage in conversation.

        Available tools:
        - updateCart: Updates an existing cart after a shipping address is provided (requires: cart_id, shipping_address)
        - findItemsWorkflow: Searches the catalog for items based on shopping intent (requires: intent_mandate)
        - initiatePayment: Initiates a payment for a given payment mandate (requires: payment_mandate, risk_data)
        - dpcFinish: Receives and validates a DPC response to finalize payment (requires: dpc_response)

        """ + SystemUtils.DEBUG_MODE_INSTRUCTIONS)
    @Override
    String selectTool(@UserMessage String prompt);
}

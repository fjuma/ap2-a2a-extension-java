package io.ap2.a2a.extension.roles.merchant.payment.processor;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.ap2.a2a.extension.common.ToolSelectorAgent;
import io.ap2.a2a.extension.common.SystemUtils;
import io.quarkiverse.langchain4j.RegisterAiService;

/**
 * LangChain4j agent for merchant payment processor tool selection.
 * This agent uses an LLM to intelligently select the appropriate tool based on the user's request.
 */
@RegisterAiService
public interface MerchantPaymentProcessorAgent extends ToolSelectorAgent {

    /**
     * System message that guides the AI in selecting the appropriate tool.
     * This is similar to the _system_prompt in the Python implementation.
     */
    @SystemMessage("""
        You are a payment processor agent. Your role is to process payments
        on behalf of a merchant.

        Based on the user's request, identify their intent and select the
        single correct tool to use. Your only output should be a tool call.
        Do not engage in conversation.

        Available tools:
        - initiatePayment: Initiates a payment for a given payment mandate (requires: payment_mandate)

        """ + SystemUtils.DEBUG_MODE_INSTRUCTIONS)
    @Override
    String selectTool(@UserMessage String prompt);
}

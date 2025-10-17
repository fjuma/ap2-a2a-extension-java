package io.ap2.a2a.extension.roles.credentials.provider;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.ap2.a2a.extension.common.ToolSelectorAgent;
import io.ap2.a2a.extension.common.SystemUtils;
import io.quarkiverse.langchain4j.RegisterAiService;

/**
 * LangChain4j agent for credentials provider tool selection.
 * This agent uses an LLM to intelligently select the appropriate tool based on the user's request.
 */
@RegisterAiService
public interface CredentialsProviderAgent extends ToolSelectorAgent {

    /**
     * System message that guides the AI in selecting the appropriate tool.
     * This is similar to the _system_prompt in the Python implementation.
     */
    @SystemMessage("""
        You are a credentials provider agent acting as a secure digital wallet.
        Your job is to manage a user's payment methods and shipping addresses.

        Based on the user's request, identify their intent and select the
        single correct tool to use. Your only output should be a tool call.
        Do not engage in conversation.

        Available tools:
        - handleGetShippingAddress: Get a user's shipping address (requires: user_email)
        - handleSearchPaymentMethods: Search for payment methods matching merchant criteria (requires: user_email, payment_method_data)
        - handleCreatePaymentCredentialToken: Create a tokenized payment credential (requires: user_email, payment_method_alias)
        - handleGetPaymentMethodRawCredentials: Exchange a payment token for raw credentials (requires: payment_mandate with token)
        - handleSignedPaymentMandate: Process a signed payment mandate (requires: payment_mandate)

        """ + SystemUtils.DEBUG_MODE_INSTRUCTIONS)
    @Override
    String selectTool(@UserMessage String prompt);
}

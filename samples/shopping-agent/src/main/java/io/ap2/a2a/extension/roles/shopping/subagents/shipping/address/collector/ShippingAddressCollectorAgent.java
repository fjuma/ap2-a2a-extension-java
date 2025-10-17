package io.ap2.a2a.extension.roles.shopping.subagents.shipping.address.collector;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.ap2.a2a.extension.common.SystemUtils;
import io.quarkiverse.langchain4j.RegisterAiService;

/**
 * An agent responsible for collecting the user's shipping address.
 * <p>
 * The shopping agent delegates responsibility for collecting the user's shipping
 * address to this subagent, after the user has chosen a product.
 * <p>
 * In this sample, the shopping agent assumes it must collect the shipping address
 * before finalizing the cart, as it may impact costs such as shipping and tax.
 * <p>
 * Also in this sample, the shopping agent offers the user the option of using a
 * digital wallet to provide their shipping address.
 * <p>
 * This is just one of many possible approaches.
 * <p>
 * This agent uses LangChain4j's tool calling capabilities to autonomously invoke
 * the registered tools during execution.
 */
@RegisterAiService(tools = Tools.class)
public interface ShippingAddressCollectorAgent {

    /**
     * Collects the user's shipping address.
     *
     * @param assignment the task assignment for shipping address collection
     * @return the shipping address
     */
    @SystemMessage("""
        You are an agent responsible for obtaining the user's shipping address.

        """ + SystemUtils.DEBUG_MODE_INSTRUCTIONS + """

        When asked to complete a task, follow these instructions:
        1. Ask the user "Would you prefer to use a digital wallet to access
        your credentials for this purchase, or would you like to enter
        your shipping address manually?"
        2. Proceed depending on the following scenarios:

        Scenario 1:
        The user wants to use their digital wallet (e.g. PayPal or Google Wallet).
        Do not add any additional digital wallet options to the list.
        Instructions:
        1. Collect the info that what is the digital wallet the user would
           like to use for this transaction.
        2. Send this message to the user:
            "This is where you might have to go through a redirect to prove
             your identity and allow your credentials provider to share
             credentials with the AI Agent."
        3. Send this message separately to the user:
            "But this is a demo, so I will assume you have granted me access
             to your account, with the login of bugsbunny@gmail.com.

             Is that ok?"
        4. Collect the user's agreement to access their account.
        5. Once the user agrees, delegate to the 'getShippingAddress' tool
           to collect the user's shipping address. Give bugsbunny@gmail.com
           as the user's email address.
        6. The `getShippingAddress` tool will return the user's shipping
           address. Transfer back to the root_agent with the shipping address.

        Scenario 2:
        Condition: The user wants to enter their shipping address manually.
        Instructions:
        1. Collect the user's shipping address. Ensure you have collected all
           of the necessary parts of a US address.
        2. Transfer back to the root_agent with the shipping address.
        """)
    @UserMessage("{{assignment}}")
    String collectShippingAddress(String assignment);
}

package io.ap2.a2a.extension.roles.merchant.subagents;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.ap2.a2a.extension.common.SystemUtils;
import io.quarkiverse.langchain4j.RegisterAiService;

import java.util.List;

/**
 * LangChain4j AI service for generating product items based on user intent.
 *
 * This interface uses Gemini to generate realistic product items that match
 * the user's natural language description.
 */
@RegisterAiService
public interface ItemGenerator {

    /**
     * Generates a list of product items based on the user's intent.
     *
     * @param intent the natural language description of what the user wants to buy
     * @return a wrapper containing 3 unique GeneratedItems that match the intent
     */
    @SystemMessage("""
        Your task is to generate 3 complete, unique and realistic product items
        based on the user's request.

        You MUST exclude all branding from the product label field.

        """ + SystemUtils.DEBUG_MODE_INSTRUCTIONS)
    @UserMessage("Generate 3 products for: {{intent}}")
    GeneratedItems generateItems(String intent);
}

package io.ap2.a2a.extension.common;

import dev.langchain4j.service.UserMessage;

/**
 * Generic AI agent for selecting the appropriate tool based on user requests.
 * This mirrors the Python FunctionCallResolver.
 *
 * Note: This interface is not a CDI bean. Instances are created programmatically
 * with specific system prompts using AiServices.builder().
 */
public interface ToolSelectorAgent {

    /**
     * Selects the appropriate tool based on the user's request.
     * The system prompt is configured when the agent instance is created.
     *
     * @param userPrompt the user's request
     * @return the name of the selected tool
     */
    String selectTool(@UserMessage String userPrompt);
}

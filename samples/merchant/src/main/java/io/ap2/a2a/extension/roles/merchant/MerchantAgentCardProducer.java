package io.ap2.a2a.extension.roles.merchant;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentExtension;
import io.a2a.spec.AgentSkill;
import io.ap2.a2a.extension.common.A2aExtensionUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.List;
import java.util.Map;

/**
 * Producer for merchant agent card configuration.
 */
@ApplicationScoped
public final class MerchantAgentCardProducer {

    /**
     * Produces the agent card for the merchant agent.
     *
     * @return the configured agent card
     */
    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return new AgentCard.Builder()
            .name("MerchantAgent")
            .description("A sales assistant agent for a merchant.")
            .url("http://localhost:8001/a2a/merchant_agent")
            .version("1.0.0")
            .capabilities(
                new AgentCapabilities.Builder()
                    .extensions(List.of(
                        new AgentExtension.Builder()
                            .uri(A2aExtensionUtils.EXTENSION_URI)
                            .description("Supports the Agent Payments Protocol.")
                            .required(true)
                            .build(),
                        new AgentExtension.Builder()
                            .uri("https://sample-card-network.github.io/paymentmethod/types/v1")
                            .description("Supports the Sample Card Network payment method extension")
                            .required(true)
                            .build()))
                    .build())
            .defaultInputModes(List.of("json"))
            .defaultOutputModes(List.of("json"))
            .skills(List.of(
                new AgentSkill.Builder()
                    .id("search_catalog")
                    .name("Search Catalog")
                    .description("Searches the merchant's catalog based on a shopping intent & returns a cart containing the top results.")
                    .tags(List.of("merchant", "search", "catalog"))
                    .build()))
            .protocolVersion("0.3.0")
            .build();
    }
}

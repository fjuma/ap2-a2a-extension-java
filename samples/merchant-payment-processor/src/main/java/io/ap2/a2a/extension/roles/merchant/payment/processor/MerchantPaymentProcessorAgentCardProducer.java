package io.ap2.a2a.extension.roles.merchant.payment.processor;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentExtension;
import io.a2a.spec.AgentSkill;
import io.ap2.a2a.extension.common.A2aExtensionUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.List;

/**
 * Producer for merchant payment processor agent card configuration.
 */
@ApplicationScoped
public final class MerchantPaymentProcessorAgentCardProducer {

    /**
     * Produces the agent card for the merchant payment processor agent.
     *
     * @return the configured agent card
     */
    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return new AgentCard.Builder()
            .name("merchant_payment_processor_agent")
            .description("An agent that processes card payments on behalf of a merchant.")
            .url("http://localhost:8003/a2a/merchant_payment_processor_agent")
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
            .defaultInputModes(List.of("text/plain"))
            .defaultOutputModes(List.of("application/json"))
            .skills(List.of(
                new AgentSkill.Builder()
                    .id("card-processor")
                    .name("Card Processor")
                    .description("Processes card payments.")
                    .tags(List.of("payment", "card"))
                    .build()))
            .protocolVersion("0.3.0")
            .build();
    }
}

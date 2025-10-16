package io.ap2.a2a.extension.roles.credentials.provider;

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
 * Producer for credentials provider agent card configuration.
 * This class is final and not designed for extension.
 */
@ApplicationScoped
public final class CredentialsProviderAgentCardProducer {

    /**
     * Produces the agent card for the credentials provider agent.
     *
     * @return the configured agent card
     */
    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return new AgentCard.Builder()
            .name("CredentialsProvider")
            .description("An agent that holds a user's payment credentials.")
            .url("http://localhost:8002/a2a/credentials_provider")
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
                    .id("initiate_payment")
                    .name("Initiate Payment")
                    .description("Initiates a payment with the correct payment processor.")
                    .tags(List.of("payments"))
                    .build(),
                new AgentSkill.Builder()
                    .id("get_eligible_payment_methods")
                    .name("Get Eligible Payment Methods")
                    .description("Provides a list of eligible payment methods for a particular purchase.")
                    // Note: AgentSkill in A2A spec doesn't support parameters field
                    // The credentials_provider_agent/agent.json spec includes parameters for email_address, but this isn't
                    // supported in the current A2A spec
                    .tags(List.of("eligible", "payment", "methods"))
                    .build(),
                new AgentSkill.Builder()
                    .id("get_account_shipping_address")
                    .name("Get Shipping Address")
                    .description("Fetches the shipping address from a user's wallet.")
                        // Note: AgentSkill in A2A spec doesn't support parameters field
                        // The credentials_provider_agent/agent.json spec includes parameters for email_address, but this isn't
                        // supported in the current A2A spec
                    .tags(List.of("account", "shipping"))
                    .build()))
            .protocolVersion("0.3.0")
            .build();
    }
}

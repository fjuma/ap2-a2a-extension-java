package io.ap2.a2a.extension.roles.shopping;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * REST endpoint for the shopping agent chat interface.
 */
@Path("/chat")
public class ChatResource {

    @Inject
    ShoppingAgent shoppingAgent;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ChatResponse chat(ChatRequest request) {
        try {
            String response = shoppingAgent.processShoppingRequest(request.message());
            return new ChatResponse(response, null);
        } catch (Exception e) {
            return new ChatResponse(null, "Error: " + e.getMessage());
        }
    }

    public record ChatRequest(String message, String sessionId) {}
    public record ChatResponse(String response, String error) {}
}

package io.ap2.a2a.extension.roles.merchant.subagents;

import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.DataPart;
import io.a2a.spec.Task;
import io.ap2.a2a.extension.common.MessageUtils;
import io.ap2.a2a.extension.roles.merchant.CartMandateStore;
import io.ap2.a2a.extension.spec.AP2Exception;
import io.ap2.a2a.extension.spec.CartContents;
import io.ap2.a2a.extension.spec.CartMandate;
import io.ap2.a2a.extension.spec.IntentMandate;
import io.ap2.a2a.extension.spec.PaymentCurrencyAmount;
import io.ap2.a2a.extension.spec.PaymentDetailsInit;
import io.ap2.a2a.extension.spec.PaymentItem;
import io.ap2.a2a.extension.spec.PaymentMethodData;
import io.ap2.a2a.extension.spec.PaymentOptions;
import io.ap2.a2a.extension.spec.PaymentRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static io.ap2.a2a.extension.spec.AP2Constants.CART_MANDATE_DATA_KEY;
import static io.ap2.a2a.extension.spec.AP2Constants.INTENT_MANDATE_DATA_KEY;

/**
 * A sub-agent that offers items from its 'catalog'.
 *
 * This agent fabricates catalog content based on the user's request using
 * LangChain4j and Gemini to generate realistic product items.
 * It mirrors the Python catalog_agent implementation.
 */
public class CatalogAgent {

    private static final Logger logger = Logger.getLogger(CatalogAgent.class.getName());

    private final CartMandateStore cartMandateStore;
    private final ItemGenerator itemGenerator;

    /**
     * Constructor for CatalogAgent.
     *
     * @param cartMandateStore the cart mandate store instance
     * @param itemGenerator the LangChain4j AI service for generating items
     */
    public CatalogAgent(CartMandateStore cartMandateStore, ItemGenerator itemGenerator) {
        this.cartMandateStore = cartMandateStore;
        this.itemGenerator = itemGenerator;
    }

    /**
     * Finds products that match the user's IntentMandate.
     *
     * This mirrors the Python find_items_workflow function.
     *
     * @param dataParts the data parts from the request
     * @param updater the task updater
     * @param currentTask the current task (not used in this function)
     * @throws AP2Exception if required data is missing or invalid
     */
    public void findItemsWorkflow(
            List<DataPart> dataParts,
            TaskUpdater updater,
            Task currentTask) throws AP2Exception {

        IntentMandate intentMandate = MessageUtils.parseCanonicalObject(
                INTENT_MANDATE_DATA_KEY, dataParts, IntentMandate.class);

        if (intentMandate == null) {
            throw new AP2Exception("Missing " + INTENT_MANDATE_DATA_KEY);
        }

        String intent = intentMandate.naturalLanguageDescription();
        logger.info("Finding items for intent: " + intent);

        // Use LangChain4j/Gemini to generate realistic items based on intent
        GeneratedItems generatedItemsWrapper = itemGenerator.generateItems(intent);
        List<GeneratedItem> generatedItems = generatedItemsWrapper.getItems();

        // Convert GeneratedItems to PaymentItems
        List<PaymentItem> items = generatedItems.stream()
                .map(gi ->new PaymentItem(
                        gi.getLabel(),
                        new PaymentCurrencyAmount(gi.getCurrency(), gi.getPrice()),
                        null,
                        null))
                .toList();

        Instant currentTime = Instant.now();
        int itemCount = 0;

        for (PaymentItem item : items) {
            itemCount++;
            createAndAddCartMandateArtifact(item, itemCount, currentTime, updater);
        }

        // Collect and add risk data
        String riskData = collectRiskData(updater);
        updater.addArtifact(List.of(new DataPart(Map.of("risk_data", riskData))));

        updater.complete();
    }

    /**
     * Creates a CartMandate and adds it as an artifact.
     *
     * This mirrors the Python _create_and_add_cart_mandate_artifact function.
     *
     * @param item the payment item
     * @param itemCount the item number (for generating unique IDs)
     * @param currentTime the current timestamp
     * @param updater the task updater
     */
    private void createAndAddCartMandateArtifact(
            PaymentItem item,
            int itemCount,
            Instant currentTime,
            TaskUpdater updater) {

        PaymentRequest paymentRequest = new PaymentRequest(
                List.of(new PaymentMethodData(
                        "CARD",
                        Map.of("network", List.of("mastercard", "paypal", "amex"))
                )),
                new PaymentDetailsInit(
                        "order_" + itemCount,
                        List.of(item),
                        null,
                        null,
                        new PaymentItem(
                                "Total",
                                item.amount(),
                                null,
                                null
                        )
                ),
                new PaymentOptions(null, null, null, true, null),
                null
        );

        Instant cartExpiry = currentTime.plus(30, ChronoUnit.MINUTES);
        CartContents cartContents = new CartContents(
                "cart_" + itemCount,
                true,
                paymentRequest,
                cartExpiry.toString(),
                "Generic Merchant"
        );

        CartMandate cartMandate = new CartMandate(cartContents, null);

        // Store the cart mandate
        cartMandateStore.setCartMandate(cartMandate.contents().id(), cartMandate);

        // Add as artifact
        updater.addArtifact(List.of(
                new DataPart(Map.of(CART_MANDATE_DATA_KEY, cartMandate))
        ));
    }

    /**
     * Creates fake risk data for demonstration purposes.
     *
     * This mirrors the Python _collect_risk_data function.
     *
     * @param updater the task updater
     * @return the fake risk data string
     */
    private String collectRiskData(TaskUpdater updater) {
        // This is fake risk data for demonstration purposes
        String riskData = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...fake_risk_data";
        cartMandateStore.setRiskData(updater.getContextId(), riskData);
        return riskData;
    }
}

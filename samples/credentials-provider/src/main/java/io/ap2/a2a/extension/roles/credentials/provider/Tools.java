package io.ap2.a2a.extension.roles.credentials.provider;

import static io.ap2.a2a.extension.spec.AP2Constants.CONTACT_ADDRESS_DATA_KEY;
import static io.ap2.a2a.extension.spec.AP2Constants.PAYMENT_MANDATE_DATA_KEY;
import static io.ap2.a2a.extension.spec.AP2Constants.PAYMENT_METHOD_DATA_DATA_KEY;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.DataPart;
import io.a2a.spec.Task;
import io.ap2.a2a.extension.common.MessageUtils;
import io.ap2.a2a.extension.spec.AP2Exception;
import io.ap2.a2a.extension.spec.ContactAddress;
import io.ap2.a2a.extension.spec.PaymentMandate;
import io.ap2.a2a.extension.spec.PaymentMethodData;

/**
 * Handles credential provider tool operations for payment processing.
 */
public class Tools {

    private final AccountManager accountManager;

    public Tools(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    /**
     * Handles a request to get the user's shipping address.
     * <p>
     * Updates a task with the user's shipping address if found.
     *
     * @param dataParts DataPart contents. Should contain a single user_email.
     * @param updater The TaskUpdater instance for updating the task state.
     * @param currentTask The current task if there is one.
     * @throws AP2Exception if user_email is not provided
     */
    public void handleGetShippingAddress(List<DataPart> dataParts, TaskUpdater updater, Task currentTask) throws AP2Exception {

        String userEmail = (String) MessageUtils.findDataPart("user_email", dataParts);
        if (userEmail == null) {
            throw new AP2Exception("user_email is required for get_shipping_address");
        }

        ContactAddress shippingAddress = accountManager.getAccountShippingAddress(userEmail);

        updater.addArtifact(List.of(new DataPart(Map.of(CONTACT_ADDRESS_DATA_KEY, shippingAddress))));
        updater.complete();
    }

    /**
     * Returns the user's payment methods that match what the merchant accepts.
     * <p>
     * The merchant's accepted payment methods are provided in the dataParts as a
     * list of PaymentMethodData objects. The user's account is identified by the
     * user_email provided in the dataParts.
     * <p>
     * This tool finds and returns all the payment methods associated with the user's
     * account that match the merchant's accepted payment methods.
     *
     * @param dataParts DataPart contents. Should contain a single user_email and a
     *                  list of PaymentMethodData objects.
     * @param updater The TaskUpdater instance for updating the task state.
     * @param currentTask The current task if there is one.
     * @throws AP2Exception if required parameters are missing
     */
    public void handleSearchPaymentMethods(
            List<DataPart> dataParts,
            TaskUpdater updater,
            Task currentTask) throws AP2Exception {

        String userEmail = (String) MessageUtils.findDataPart("user_email", dataParts);
        List<Map<String, Object>> methodData = MessageUtils.findDataParts(PAYMENT_METHOD_DATA_DATA_KEY, dataParts);

        if (userEmail == null) {
            throw new AP2Exception("user_email is required for search_payment_methods");
        }
        if (methodData == null || methodData.isEmpty()) {
            throw new AP2Exception("method_data is required for search_payment_methods");
        }

        // TODO determine how to populated supportedMethods
        String SUPPORTED_METHODS_PLACEHOLDER = "";
        List<PaymentMethodData> merchantMethodDataList = methodData.stream()
                .map(data -> new PaymentMethodData(SUPPORTED_METHODS_PLACEHOLDER, Map.of(PAYMENT_METHOD_DATA_DATA_KEY, data)))
                .collect(Collectors.toList());

        Map<String, Object> eligibleAliases = getEligiblePaymentMethodAliases(userEmail, merchantMethodDataList);

        updater.addArtifact(List.of(new DataPart(eligibleAliases)));
        updater.complete();
    }

    /**
     * Exchanges a payment token for the payment method's raw credentials.
     * <p>
     * Updates a task with the payment credentials.
     *
     * @param dataParts DataPart contents. Should contain a single PaymentMandate.
     * @param updater The TaskUpdater instance for updating the task state.
     * @param currentTask The current task if there is one.
     * @throws AP2Exception if payment method is not found
     */
    public void handleGetPaymentMethodRawCredentials(
            List<DataPart> dataParts,
            TaskUpdater updater,
            Task currentTask) throws AP2Exception {

        PaymentMandate paymentMandate = MessageUtils.parseCanonicalObject(
                PAYMENT_MANDATE_DATA_KEY, dataParts, PaymentMandate.class);

        Map<String, Object> tokenMap = (Map<String, Object>) paymentMandate.paymentMandateContents()
                .paymentResponse().details().get("token");
        String token = tokenMap != null ? (String) tokenMap.get("value") : null;
        String paymentMandateId = paymentMandate.paymentMandateContents().paymentMandateId();

        PaymentMethodData paymentMethod = accountManager.verifyToken(token, paymentMandateId);
        if (paymentMethod == null) {
            throw new AP2Exception("Payment method not found for token: " + token);
        }

        updater.addArtifact(List.of(new DataPart(paymentMethod.data())));
        updater.complete();
    }

    /**
     * Handles a request to get a payment credential token.
     * <p>
     * Updates a task with the payment credential token.
     *
     * @param dataParts DataPart contents. Should contain the user_email and
     *                  payment_method_alias.
     * @param updater The TaskUpdater instance for updating the task state.
     * @param currentTask The current task if there is one.
     * @return A CompletableFuture that completes when the operation is done
     * @throws AP2Exception if required parameters are missing
     */
    public void handleCreatePaymentCredentialToken(
            List<DataPart> dataParts,
            TaskUpdater updater,
            Task currentTask) throws AP2Exception {

        String userEmail = (String) MessageUtils.findDataPart("user_email", dataParts);
        String paymentMethodAlias = (String) MessageUtils.findDataPart("payment_method_alias", dataParts);

        if (userEmail == null || paymentMethodAlias == null) {
            throw new AP2Exception(
                    "user_email and payment_method_alias are required for create_payment_credential_token");
        }

        String tokenizedPaymentMethod = accountManager.createToken(userEmail, paymentMethodAlias);

        updater.addArtifact(List.of(new DataPart(Map.of("token", tokenizedPaymentMethod))));
        updater.complete();
    }

    /**
     * Handles a signed payment mandate.
     * <p>
     * Adds the payment mandate id to the token in storage and then completes the task.
     *
     * @param dataParts DataPart contents. Should contain a single PaymentMandate.
     * @param updater The TaskUpdater instance for updating the task state.
     * @param currentTask The current task if there is one.
     * @throws AP2Exception if there's an error processing the mandate
     */
    public void handleSignedPaymentMandate(
            List<DataPart> dataParts,
            TaskUpdater updater,
            Task currentTask) throws AP2Exception {

        PaymentMandate paymentMandate = MessageUtils.parseCanonicalObject(
                PAYMENT_MANDATE_DATA_KEY, dataParts, PaymentMandate.class);

        Map<String, Object> tokenMap = (Map<String, Object>) paymentMandate.paymentMandateContents()
                .paymentResponse().details().get("token");
        String token = tokenMap != null ? (String) tokenMap.get("value") : null;
        String paymentMandateId = paymentMandate.paymentMandateContents().paymentMandateId();

        accountManager.updateToken(token, paymentMandateId);
        updater.complete();
    }

    /**
     * Gets the payment method aliases from a list of payment methods.
     *
     * @param paymentMethods A list of payment methods
     * @return A list of payment method aliases
     */
    private List<String> getPaymentMethodAliases(List<PaymentMethodData> paymentMethods) {
        return paymentMethods.stream()
                .map(pm -> (String) pm.data().get("alias"))
                .collect(Collectors.toList());
    }

    /**
     * Gets the payment_methods eligible according to given PaymentMethodData.
     *
     * @param userEmail The email address of the user's account
     * @param merchantAcceptedPaymentMethods A list of eligible payment method criteria
     * @return A map containing the list of eligible payment method aliases
     */
    private Map<String, Object> getEligiblePaymentMethodAliases(
            String userEmail, List<PaymentMethodData> merchantAcceptedPaymentMethods) {

        List<PaymentMethodData> paymentMethods = accountManager.getAccountPaymentMethods(userEmail);
        List<PaymentMethodData> eligiblePaymentMethods = paymentMethods.stream()
                .filter(paymentMethod -> merchantAcceptedPaymentMethods.stream()
                        .anyMatch(criteria -> isPaymentMethodEligible(paymentMethod, criteria)))
                .collect(Collectors.toList());

        return Map.of("payment_method_aliases", getPaymentMethodAliases(eligiblePaymentMethods));
    }

    /**
     * Checks if a payment method is eligible based on a PaymentMethodData.
     *
     * @param paymentMethod A payment method to check
     * @param merchantCriteria A PaymentMethodData object containing the eligibility criteria
     * @return {@code true} if the payment method is eligible according to the criteria,
     *         {@code false} otherwise
     */
    private boolean isPaymentMethodEligible(PaymentMethodData paymentMethod, PaymentMethodData merchantCriteria) {
        if (!paymentMethod.supportedMethods().equals(merchantCriteria.supportedMethods())) {
            return false;
        }

        List<Object> merchantNetworks = (List<Object>) merchantCriteria.data().get("network");
        if (merchantNetworks == null || merchantNetworks.isEmpty()) {
            return false;
        }

        List<String> merchantSupportedNetworks = merchantNetworks.stream()
                .map(network -> network.toString().toLowerCase())
                .collect(Collectors.toList());

        List<Map<String, Object>> paymentCardNetworks =
                (List<Map<String, Object>>) paymentMethod.data().get("network");

        if (paymentCardNetworks == null) {
            return false;
        }

        for (Map<String, Object> networkInfo : paymentCardNetworks) {
            String networkName = (String) networkInfo.get("name");
            if (networkName != null) {
                for (String supportedNetwork : merchantSupportedNetworks) {
                    if (networkName.toLowerCase().equals(supportedNetwork)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}

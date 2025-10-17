package io.ap2.a2a.extension.common;

import java.util.logging.Logger;

import io.ap2.a2a.extension.spec.PaymentMandate;

/**
 * Validation logic for PaymentMandate.
 */
public class Validation {

    private static final Logger logger = Logger.getLogger(Validation.class.getName());

    /**
     * Validates the PaymentMandate signature.
     *
     * @param paymentMandate The PaymentMandate to be validated
     * @throws IllegalArgumentException if the PaymentMandate signature is not valid
     */
    public static void validatePaymentMandateSignature(PaymentMandate paymentMandate) {
        // In a real implementation, full validation logic would reside here. For
        // demonstration purposes, we simply log that the authorization field is
        // populated.
        if (paymentMandate.userAuthorization() == null) {
            throw new IllegalArgumentException("User authorization not found in PaymentMandate.");
        }

        logger.info("Valid PaymentMandate found.");
    }

    private Validation() {
        // Utility class should not be instantiated
    }
}

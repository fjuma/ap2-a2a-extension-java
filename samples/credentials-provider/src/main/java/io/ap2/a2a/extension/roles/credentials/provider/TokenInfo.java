package io.ap2.a2a.extension.roles.credentials.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.ap2.a2a.extension.util.Assert;

/**
 * Represents token information for payment credentials.
 */
class TokenInfo {

    private final String emailAddress;
    private final String paymentMethodAlias;
    private String paymentMandateId;

    public TokenInfo(String emailAddress, String paymentMethodAlias) {
        this(emailAddress, paymentMethodAlias, null);
    }

    public TokenInfo(String emailAddress, String paymentMethodAlias, String paymentMandateId) {
        Assert.checkNotNullParam("emailAddress", emailAddress);
        Assert.checkNotNullParam("paymentMethodAlias", paymentMethodAlias);
        this.emailAddress = emailAddress;
        this.paymentMethodAlias = paymentMethodAlias;
        this.paymentMandateId = paymentMandateId;
    }

    public String getPaymentMandateId() {
        return paymentMandateId;
    }

    public void setPaymentMandateId(String paymentMandateId) {
        this.paymentMandateId = paymentMandateId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getPaymentMethodAlias() {
        return paymentMethodAlias;
    }
}

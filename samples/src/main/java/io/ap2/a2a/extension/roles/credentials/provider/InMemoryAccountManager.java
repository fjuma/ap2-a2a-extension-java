package io.ap2.a2a.extension.roles.credentials.provider;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.ap2.a2a.extension.spec.AP2Exception;
import io.ap2.a2a.extension.spec.ContactAddress;
import io.ap2.a2a.extension.spec.PaymentMethodData;

/**
 * An in-memory manager of a user's 'account details'.
 *
 * Each 'account' contains a user's payment methods and shipping address.
 */
@ApplicationScoped
public class InMemoryAccountManager implements AccountManager{

    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    @Override
    public String createToken(String emailAddress, String paymentMethodAlias) {
        String token = "fake_payment_credential_token_" + tokens.size();
        TokenInfo tokenInfo = new TokenInfo(emailAddress, paymentMethodAlias, null);
        tokens.put(token, tokenInfo);
        return token;
    }

    @Override
    public void updateToken(String token, String paymentMandateId) throws AP2Exception {
        if (!tokens.containsKey(token)) {
            throw new AP2Exception("Token " + token + " not found");
        }

        TokenInfo tokenInfo = tokens.get(token);
        if (tokenInfo.getPaymentMandateId() != null) {
            // Do not overwrite the payment mandate id if it is already set
            return;
        }

        tokenInfo.setPaymentMandateId(paymentMandateId);
        tokens.put(token, tokenInfo);
    }

    @Override
    public PaymentMethodData verifyToken(String token, String paymentMandateId) throws AP2Exception {
        TokenInfo tokenInfo = tokens.get(token);

        if (tokenInfo == null) {
            throw new AP2Exception("Invalid token");
        }

        if (!tokenInfo.getPaymentMandateId().equals(paymentMandateId)) {
            throw new AP2Exception("Invalid token");
        }

        String emailAddress = tokenInfo.getEmailAddress();
        String alias = tokenInfo.getPaymentMethodAlias();
        return getPaymentMethodByAlias(emailAddress, alias);
    }

    @Override
    public List<PaymentMethodData> getAccountPaymentMethods(String emailAddress) {
        Account account = accounts.get(emailAddress);
        if (account == null || account.paymentMethods() == null) {
            return null;
        }
        return account.paymentMethods();
    }

    @Override
    public ContactAddress getAccountShippingAddress(String emailAddress) {
        Account account = accounts.get(emailAddress);
        if (account == null || account.shippingAddress() == null) {
            return null;
        }
        return account.shippingAddress();
    }

    @Override
    public PaymentMethodData getPaymentMethodByAlias(String emailAddress, String alias) {
        List<PaymentMethodData> paymentMethods = getAccountPaymentMethods(emailAddress);

        List<PaymentMethodData> matchingPaymentMethods = paymentMethods.stream()
                .filter(paymentMethod -> {
                    String paymentMethodAlias = (String) paymentMethod.data().get("alias");
                    return paymentMethodAlias != null && paymentMethodAlias.equalsIgnoreCase(alias);
                })
                .toList();

        if (matchingPaymentMethods.isEmpty()) {
            return null;
        }

        return matchingPaymentMethods.get(0);
    }

}

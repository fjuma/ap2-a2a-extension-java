package io.ap2.a2a.extension.roles.credentials.provider;

import static io.ap2.a2a.extension.util.Assert.checkNotNullParam;

import java.util.List;

import io.ap2.a2a.extension.spec.ContactAddress;
import io.ap2.a2a.extension.spec.PaymentMethodData;

record Account(String emailAddress, ContactAddress shippingAddress, List<PaymentMethodData> paymentMethods) {

    public Account {
        checkNotNullParam("emailAddress", emailAddress);
    }
}

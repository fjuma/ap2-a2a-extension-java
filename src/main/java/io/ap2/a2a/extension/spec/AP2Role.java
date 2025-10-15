package io.ap2.a2a.extension.spec;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AP2Role {

    MERCHANT("merchant"),
    SHOPPER("shopper"),
    CREDENTIALS_PROVIDER("credentials-provider"),
    PAYMENT_PROCESSOR("payment-processor");

    private String ap2Role;

    AP2Role(String ap2Role) {
        this.ap2Role = ap2Role;
    }

    @JsonValue
    public String asString() {
        return ap2Role;
    }
}

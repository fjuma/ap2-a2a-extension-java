package io.ap2.a2a.extension.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AP2ExtensionParameters(List<AP2Role> roles) {

    public AP2ExtensionParameters {
        Assert.checkNotNullParam("roles", roles);
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }
    }

}

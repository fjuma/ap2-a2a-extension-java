package io.ap2.a2a.extension.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The ContactAddress interface represents a physical address.
 * <p>
 * Specification:
 * https://www.w3.org/TR/contact-picker/#contact-address
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContactAddress(
        String city,
        String country,
        @JsonProperty("dependent_locality") String dependentLocality,
        String organization,
        @JsonProperty("phone_number") String phoneNumber,
        @JsonProperty("postal_code") String postalCode,
        String recipient,
        String region,
        @JsonProperty("sorting_code") String sortingCode,
        @JsonProperty("address_line") List<String> addressLine
) {
}

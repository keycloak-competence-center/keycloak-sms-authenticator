package com.inventage.keycloak.sms.authentication;

/**
 * Utility for normalizing phone numbers before validation and storage.
 *
 * <p>Cleaning is applied <em>before</em> regex validation, so the validation regex
 * should be written against the cleaned form (e.g. {@code ^\+417[5-9]\d{7}$}
 * rather than expecting spaces or a {@code 00} prefix).</p>
 */
public class PhoneNumberUtils {

    private PhoneNumberUtils() {
    }

    /**
     * Strips whitespace, dashes, and parentheses from the input and
     * converts the international {@code 00} prefix to {@code +}.
     */
    public static String clean(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String cleaned = phoneNumber.replaceAll("[\\s\\-()]+", "");
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
        }
        return cleaned;
    }
}

package com.inventage.keycloak.sms.authentication;

/**
 * Result of validating an SMS code against the stored challenge.
 */
public enum SmsCodeValidationResult {

    VALID(null),
    EMPTY("smsCodeErrorEmpty"),
    WRONG("smsCodeErrorWrong"),
    EXPIRED("smsCodeErrorExpired");

    private final String messageKey;

    SmsCodeValidationResult(String messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * Returns the i18n message key for this validation error, or null if valid.
     */
    public String messageKey() {
        return messageKey;
    }
}

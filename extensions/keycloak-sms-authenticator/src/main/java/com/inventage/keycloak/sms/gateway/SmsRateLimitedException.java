package com.inventage.keycloak.sms.gateway;

/**
 * Thrown by {@link SmsService} implementations when the SMS gateway rejects
 * the request due to rate limiting (e.g. HTTP 429).
 */
public class SmsRateLimitedException extends RuntimeException {

    public SmsRateLimitedException(String message) {
        super(message);
    }

    public SmsRateLimitedException(String message, Throwable cause) {
        super(message, cause);
    }
}

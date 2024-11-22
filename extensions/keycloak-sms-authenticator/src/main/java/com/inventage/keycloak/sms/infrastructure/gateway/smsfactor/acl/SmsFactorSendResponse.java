package com.inventage.keycloak.sms.infrastructure.gateway.smsfactor.acl;

/**
 * DTO for the <a href="https://dev.smsfactor.com/en/api/sms/send/send-single#:~:text=Accept%3A%20application/json-,Result%20Format,-A%20status%20%2D8">response from SMSFactor</a>
 */
public record SmsFactorSendResponse(
        int status,
        String message,
        String ticket,
        int cost,
        int credits,
        int total,
        int sent,
        int blacklisted,
        int duplicated,
        int napi,
        int invalid,
        int not_allowed,
        int flood,
        int country_limit,
        String details
) {
}

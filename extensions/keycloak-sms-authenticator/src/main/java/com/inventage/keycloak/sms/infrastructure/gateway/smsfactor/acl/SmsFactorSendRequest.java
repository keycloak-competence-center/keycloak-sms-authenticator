package com.inventage.keycloak.sms.infrastructure.gateway.smsfactor.acl;

/**
 * An instance of this class represents an SMS request to <a href="https://www.smsfactor.com/">SMS Factor</a>.
 *
 * @param recipient mobile number of recipient
 * @param messageContent content of the message
 * @param sender name of the sender
 */
public record SmsFactorSendRequest(String recipient, String messageContent, String sender) {

}

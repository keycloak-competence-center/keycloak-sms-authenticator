package com.inventage.keycloak.sms.infrastructure.gateway.smsfactor.acl;

/**
 * Translator for the <a href="https://www.smsfactor.com/">SMS Factor</a> API according to the DDD anti-corruption-layer pattern.
 */
public class SmsFactorTranslator {

    public SmsFactorSendRequest smsRequest(String phoneNumber, String message, String sender) {
        return new SmsFactorSendRequest(phoneNumber, message, sender);
    }
}

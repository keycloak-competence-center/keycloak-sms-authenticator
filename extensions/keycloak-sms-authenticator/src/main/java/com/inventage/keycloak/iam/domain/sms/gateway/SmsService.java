package com.inventage.keycloak.iam.domain.sms.gateway;

/**
 * @author Geraldine von Roten
 */
public interface SmsService {

    void send(String phoneNumber, String message);

}
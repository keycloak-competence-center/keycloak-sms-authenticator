package com.inventage.keycloak.sms.gateway;

/**
 * @author Geraldine von Roten
 */
public interface SmsService {

    void send(String phoneNumber, String message);

}
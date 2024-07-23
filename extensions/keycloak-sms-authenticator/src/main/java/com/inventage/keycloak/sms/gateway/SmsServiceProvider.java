package com.inventage.keycloak.sms.gateway;

import org.keycloak.provider.Provider;

/**
 * This class can provide an instance for SmsService.
 */
public interface SmsServiceProvider extends Provider {

    SmsService getSmsService();
}

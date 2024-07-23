package com.inventage.keycloak.sms.infrastructure.gateway.console;

import com.inventage.keycloak.sms.gateway.SmsService;
import com.inventage.keycloak.sms.gateway.SmsServiceProvider;
import org.jboss.logging.Logger;

public class SmsToConsoleProvider implements SmsService, SmsServiceProvider {

    private static final Logger LOG = Logger.getLogger(SmsToConsoleProvider.class);

    @Override
    public void send(String phoneNumber, String message) {
        LOG.warn(String.format("SMS for '%s' with text: '%s'", phoneNumber, message));
    }

    @Override
    public SmsService getSmsService() {
        return this;
    }

    @Override
    public void close() {
    }
}

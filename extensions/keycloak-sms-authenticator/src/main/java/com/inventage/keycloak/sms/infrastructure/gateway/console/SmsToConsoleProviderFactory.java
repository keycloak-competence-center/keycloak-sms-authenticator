package com.inventage.keycloak.sms.infrastructure.gateway.console;

import com.inventage.keycloak.sms.gateway.SmsServiceProvider;
import com.inventage.keycloak.sms.gateway.SmsServiceProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class SmsToConsoleProviderFactory implements SmsServiceProviderFactory {

    public static final String PROVIDER_ID = "sms-to-console";

    @Override
    public SmsServiceProvider create(KeycloakSession keycloakSession) {
        return new SmsToConsoleProvider();
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}

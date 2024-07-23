package com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.requireaction;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class SmsRequiredActionFactory implements RequiredActionFactory {

    private static final SmsRequiredAction SINGLETON = new SmsRequiredAction();

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return SINGLETON;
    }


    @Override
    public String getId() {
        return SmsRequiredAction.PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "SMS";
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

}

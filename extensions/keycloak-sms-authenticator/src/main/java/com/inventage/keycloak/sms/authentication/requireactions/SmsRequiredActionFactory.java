package com.inventage.keycloak.sms.authentication.requireactions;

import com.inventage.keycloak.sms.authentication.SmsCodeConfiguration;
import com.inventage.keycloak.sms.theme.SmsTextService;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class SmsRequiredActionFactory implements RequiredActionFactory {

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return new SmsRequiredAction(new SmsTextService(session));
    }


    @Override
    public String getId() {
        return SmsRequiredAction.PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "Configure SMS";
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return SmsCodeConfiguration.getConfigProperties();
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

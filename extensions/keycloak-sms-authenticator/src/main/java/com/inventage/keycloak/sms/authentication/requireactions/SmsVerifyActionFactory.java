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

/**
 * Factory for {@link SmsVerifyAction}, a required action that verifies an existing SMS credential.
 */
public class SmsVerifyActionFactory implements RequiredActionFactory {

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return new SmsVerifyAction(new SmsTextService(session));
    }

    @Override
    public String getId() {
        return SmsVerifyAction.PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "Verify SMS";
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return SmsCodeConfiguration.getCodeConfigProperties();
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

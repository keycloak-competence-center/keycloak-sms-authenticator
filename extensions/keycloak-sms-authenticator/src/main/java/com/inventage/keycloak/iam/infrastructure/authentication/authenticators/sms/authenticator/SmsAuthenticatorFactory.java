package com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

import com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.credential.SmsCredentialModel;

/**
 * @author Geraldine von Roten
 */
public class SmsAuthenticatorFactory implements AuthenticatorFactory {

    @Override
    public String getId() {
        return "sms-authenticator";
    }

    @Override
    public String getDisplayType() {
        return "SMS Authentication";
    }

    @Override
    public String getHelpText() {
        return "Validates an OTP sent via SMS to the users mobile phone.";
    }

    @Override
    public String getReferenceCategory() {
        return SmsCredentialModel.TYPE;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                AuthenticationExecutionModel.Requirement.DISABLED,
        };
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new SmsAuthenticator();
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
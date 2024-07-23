package com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.credential;

import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.KeycloakSession;

public class SmsCredentialProviderFactory implements CredentialProviderFactory<SmsCredentialProvider>{

    public static final String PROVIDER_ID = "sms-provider";

    @Override
    public CredentialProvider<SmsCredentialModel> create(KeycloakSession session) {
        return new SmsCredentialProvider(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}

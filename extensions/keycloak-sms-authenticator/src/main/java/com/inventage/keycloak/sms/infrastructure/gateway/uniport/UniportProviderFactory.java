package com.inventage.keycloak.sms.infrastructure.gateway.uniport;

import com.inventage.keycloak.sms.gateway.SmsServiceProvider;
import com.inventage.keycloak.sms.gateway.SmsServiceProviderFactory;
import org.keycloak.Config;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

import static com.inventage.keycloak.sms.infrastructure.gateway.uniport.UniportProvider.*;

public class UniportProviderFactory implements SmsServiceProviderFactory, ComponentFactory<SmsServiceProvider, SmsServiceProvider> {

    public static final String PROVIDER_ID = "uniport-sms-service";

    @Override
    public SmsServiceProvider create(KeycloakSession keycloakSession, ComponentModel model) {
        return new UniportProvider(keycloakSession);
    }

    @Override
    public SmsServiceProvider create(KeycloakSession keycloakSession) {
        return new UniportProvider(keycloakSession);
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

    @Override
    public String getHelpText() {
        return "";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        final ProviderConfigProperty serviceUrl = new ProviderConfigProperty();
        serviceUrl.setType(ProviderConfigProperty.STRING_TYPE);
        serviceUrl.setDefaultValue(SERVICE_URL_DEFAULT);
        serviceUrl.setName(SERVICE_URL_CONFIG);
        serviceUrl.setLabel("Service URL");
        serviceUrl.setHelpText("URL of the service endpoint.");

        final ProviderConfigProperty senderAddress = new ProviderConfigProperty();
        senderAddress.setType(ProviderConfigProperty.STRING_TYPE);
        senderAddress.setDefaultValue(SENDER_ADDRESS_DEFAULT);
        senderAddress.setName(SENDER_ADDRESS_CONFIG);
        senderAddress.setLabel("Sender Address");
        senderAddress.setHelpText("Address used as the sender of the SMS.");

        return List.of(serviceUrl, senderAddress);
    }
}

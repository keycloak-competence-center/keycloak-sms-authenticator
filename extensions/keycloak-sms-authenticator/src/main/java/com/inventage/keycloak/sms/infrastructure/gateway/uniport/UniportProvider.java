package com.inventage.keycloak.sms.infrastructure.gateway.uniport;

import com.inventage.keycloak.sms.gateway.SmsService;
import com.inventage.keycloak.sms.gateway.SmsServiceProvider;
import com.inventage.keycloak.sms.infrastructure.gateway.uniport.acl.UniportSmsAdapter;
import com.inventage.keycloak.sms.infrastructure.gateway.uniport.acl.UniportSmsFacade;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

public class UniportProvider implements SmsServiceProvider {

    public static final String SERVICE_URL_DEFAULT = "http://localhost:20102/sms/send";
    public static final String SERVICE_URL_CONFIG = "service-url";

    public static final String SENDER_ADDRESS_DEFAULT = "Keycloak";
    public static final String SENDER_ADDRESS_CONFIG = "sender-address";
   
    private final KeycloakSession session;
    private final Optional<ComponentModel> componentModel;

    public UniportProvider(KeycloakSession session) {
        this.session = session;
        this.componentModel = getComponentModel();
    }

    @Override
    public SmsService getSmsService() {
        return new UniportSmsAdapter(getSenderAddress(), createUniportSmsFacade());
    }

    @Override
    public void close() {

    }

    private String getSenderAddress() {
        if (componentModel.isPresent()) {
            return componentModel.get().get(SENDER_ADDRESS_CONFIG);
        }
        return SENDER_ADDRESS_DEFAULT;
    }

    private UniportSmsFacade createUniportSmsFacade() {
        String serviceUrl = SERVICE_URL_DEFAULT;
        if (componentModel.isPresent()) {
            serviceUrl = componentModel.get().get(SENDER_ADDRESS_CONFIG);
        }
        return new UniportSmsFacade(session.getProvider(HttpClientProvider.class).getHttpClient(), serviceUrl);
    }



    private Optional<ComponentModel> getComponentModel() {
        return session.getContext().getRealm().getComponentsStream()
                .filter(componentModel -> componentModel.getProviderId().equals(UniportProviderFactory.PROVIDER_ID))
                .findFirst();
    }

}

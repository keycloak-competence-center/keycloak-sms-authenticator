package com.inventage.keycloak.sms.infrastructure.gateway.smsfactor;

import com.inventage.keycloak.sms.gateway.SmsService;
import com.inventage.keycloak.sms.gateway.SmsServiceProvider;
import com.inventage.keycloak.sms.infrastructure.gateway.smsfactor.acl.SmsFactorAdapter;
import com.inventage.keycloak.sms.infrastructure.gateway.smsfactor.acl.SmsFactorFacade;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

/**
 * {@inheritDoc}
 *
 * {@link SmsService} provider for <a href="https://www.smsfactor.com/">SMS Factor</a>.
 */
public class SmsFactorProvider implements SmsServiceProvider {

    /* package private */ static final String SMS_FACTOR_SERVICE_URL_DEFAULT = "https://api.smsfactor.com/send";
    /* package private */ static final String SMS_FACTOR_SERVICE_URL_CONFIG = "service-url";

    /* package private */ static final String SMS_FACTOR_SENDER_ADDRESS_CONFIG = "sender-address";
    /* package private */ static final String SMS_FACTOR_SENDER_ADDRESS_DEFAULT = "Keycloak";

    /* package private */ static final String SMS_FACTOR_TOKEN_CONFIG = "sms-factor-token";
    /* package private */ static final String SMS_FACTOR_TOKEN_DEFAULT = "token";

    private final KeycloakSession session;
    private final Optional<ComponentModel> componentModel;
    private final SmsFactorAdapter smsFactorAdapter;

    public SmsFactorProvider(KeycloakSession session) {
        this.session = session;
        this.componentModel = getComponentModel();
        this.smsFactorAdapter = new SmsFactorAdapter(getSenderAddress(), createSmsFactorFacade());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SmsService getSmsService() {
        return smsFactorAdapter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // NOP
    }

    private String getSenderAddress() {
        if (componentModel.isPresent()) {
            return componentModel.get().get(SMS_FACTOR_SENDER_ADDRESS_CONFIG);
        }
        return SMS_FACTOR_SENDER_ADDRESS_DEFAULT;
    }

    private Optional<ComponentModel> getComponentModel() {
        return session.getContext().getRealm().getComponentsStream()
                .filter(componentModel -> componentModel.getProviderId().equals(SmsFactorProviderFactory.PROVIDER_ID))
                .findFirst();
    }

    private String getSmsFactorToken() {
        if (componentModel.isPresent()) {
            return componentModel.get().get(SMS_FACTOR_TOKEN_CONFIG);
        }

        return SMS_FACTOR_TOKEN_DEFAULT;
    }

    private SmsFactorFacade createSmsFactorFacade() {
        String serviceUrl = SMS_FACTOR_SERVICE_URL_DEFAULT;
        if (componentModel.isPresent()) {
            serviceUrl = componentModel.get().get(SMS_FACTOR_SERVICE_URL_CONFIG);
        }
        return new SmsFactorFacade(session.getProvider(HttpClientProvider.class).getHttpClient(), serviceUrl, getSmsFactorToken());
    }
}

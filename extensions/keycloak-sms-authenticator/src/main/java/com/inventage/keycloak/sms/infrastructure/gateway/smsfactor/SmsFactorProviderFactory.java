package com.inventage.keycloak.sms.infrastructure.gateway.smsfactor;

import com.inventage.keycloak.sms.gateway.SmsServiceProvider;
import com.inventage.keycloak.sms.gateway.SmsServiceProviderFactory;
import org.keycloak.Config;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

import static com.inventage.keycloak.sms.infrastructure.gateway.smsfactor.SmsFactorProvider.*;

/**
 * {@inheritDoc}
 *
 * Creates a {@link SmsServiceProvider} for <a href="https://www.smsfactor.com/">SMS Factor</a>.
 * The {@link SmsFactorProviderFactory#getConfigProperties()} provides the available configuration properties.
 *
 */
public class SmsFactorProviderFactory implements SmsServiceProviderFactory, ComponentFactory<SmsServiceProvider, SmsServiceProvider> {

    /* package private */ static final String PROVIDER_ID = "sms-factor";

    private static SmsFactorProvider smsFactorProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public SmsServiceProvider create(KeycloakSession session, ComponentModel model) {
        if (smsFactorProvider == null) {
            smsFactorProvider = new SmsFactorProvider(session);
        }

        return smsFactorProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SmsServiceProvider create(KeycloakSession session) {
        if (smsFactorProvider == null) {
            smsFactorProvider = new SmsFactorProvider(session);
        }

        return smsFactorProvider;
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHelpText() {
        return "SMS Factor Provider";
    }

    /**
     * {@inheritDoc}
     *
     * The {@link SmsFactorProvider} provides following configurations:
     *
     * <ul>
     *     <li><b>service-url</b>, default: {@link SmsFactorProvider#SMS_FACTOR_SERVICE_URL_DEFAULT}</li>
     *     <li><b>sender-address</b>, default: {@link SmsFactorProvider#SMS_FACTOR_SENDER_ADDRESS_DEFAULT}</li>
     *     <li><b>sms-factor-token</b>, default: {@link SmsFactorProvider#SMS_FACTOR_TOKEN_DEFAULT}</li>
     * </ul>
     *
     * An example for configuration:
     *
     * <pre>
     *     {@code "components": {
     *     "com.inventage.keycloak.sms.gateway.SmsServiceProvider": [
     *       {
     *         "name": "SmsFactorProvider",
     *         "providerId": "sms-factor",
     *         "subComponents": {},
     *         "config": {
     *           "service-url": [
     *             "https://api.smsfactor.com/send/simulate"
     *           ],
     *           "sender-address": [
     *             "Keycloak"
     *           ],
     *           "sms-factor-token": [
     *             "token"
     *           ]
     *         }
     *       }
     *     ]
     *   }
     *   }
     * </pre>
     */
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        final ProviderConfigProperty serviceUrl = new ProviderConfigProperty();
        serviceUrl.setType(ProviderConfigProperty.STRING_TYPE);
        serviceUrl.setDefaultValue(SMS_FACTOR_SERVICE_URL_DEFAULT);
        serviceUrl.setName(SMS_FACTOR_SERVICE_URL_CONFIG);
        serviceUrl.setLabel("SMS Factor Base URL");
        serviceUrl.setHelpText("URL of the SMS Factor endpoint.");

        final ProviderConfigProperty senderAddress = new ProviderConfigProperty();
        senderAddress.setType(ProviderConfigProperty.STRING_TYPE);
        senderAddress.setDefaultValue(SMS_FACTOR_SENDER_ADDRESS_DEFAULT);
        senderAddress.setName(SMS_FACTOR_SENDER_ADDRESS_CONFIG);
        senderAddress.setLabel("Sender");
        senderAddress.setHelpText("Name used as the sender of the SMS.");

        final ProviderConfigProperty token = new ProviderConfigProperty();
        token.setType(ProviderConfigProperty.STRING_TYPE);
        token.setDefaultValue(SMS_FACTOR_TOKEN_DEFAULT);
        token.setName(SMS_FACTOR_TOKEN_CONFIG);
        token.setLabel("SMS Factor Token");
        token.setHelpText("SMS Factor token used to send SMSes");

        return List.of(serviceUrl, senderAddress, token);
    }
}

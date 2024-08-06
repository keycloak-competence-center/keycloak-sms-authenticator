package com.inventage.keycloak.sms.infrastructure.gateway.smsfactor.acl;

import com.inventage.keycloak.sms.gateway.SmsService;
import org.apache.http.HttpResponse;
import org.jboss.logging.Logger;

/**
 * {@inheritDoc}
 *
 * {@link SmsService} implementation for <a href="https://www.smsfactor.com/">SMS Factor</a> according to the DDD anti-corruption-layer pattern.
 */
public class SmsFactorAdapter implements SmsService {

    private static final Logger LOGGER = Logger.getLogger(SmsFactorAdapter.class);

    private final String sender;
    private final SmsFactorTranslator translator;
    private final SmsFactorFacade facade;

    public SmsFactorAdapter(String sender, SmsFactorFacade facade) {
        this.sender = sender;
        this.translator = new SmsFactorTranslator();
        this.facade = facade;
    }

    @Override
    public void send(String phoneNumber, String message) {
        final SmsFactorSendRequest sendRequest = translator.smsRequest(phoneNumber, message, sender);

        LOGGER.debugf("send: phone Number: %s", phoneNumber);
        // TODO: do not log message
        LOGGER.debugf("send: message %s", message);
        LOGGER.debugf("send: sender %s", sender);

        final HttpResponse response = facade.get(sendRequest);

        LOGGER.infof("send: Sent SMS and got response %d", response.getStatusLine().getStatusCode());
    }
}

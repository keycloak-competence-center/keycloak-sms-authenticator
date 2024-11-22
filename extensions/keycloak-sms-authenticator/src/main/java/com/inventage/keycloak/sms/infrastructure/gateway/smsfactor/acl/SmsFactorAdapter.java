package com.inventage.keycloak.sms.infrastructure.gateway.smsfactor.acl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventage.keycloak.sms.gateway.SmsService;
import org.apache.http.HttpResponse;
import org.jboss.logging.Logger;

import java.io.IOException;

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
        LOGGER.debugf("SmsFactorAdapter: Creating SMSFactor Adapter with sender '%s'", sender);

        this.sender = sender;
        this.translator = new SmsFactorTranslator();
        this.facade = facade;
    }

    @Override
    public void send(String phoneNumber, String message) {
        final SmsFactorSendRequest sendRequest = translator.smsRequest(phoneNumber, message, sender);

        LOGGER.debugf("send: phone Number: '%s', sender: '%s'", phoneNumber, sender);

        final HttpResponse response = facade.sendSingleMessage(sendRequest);
        processResponse(response);

        LOGGER.debugf("send: Sent SMS and got '%d' HTTP status code", response.getStatusLine().getStatusCode());
    }

    private void processResponse(HttpResponse response) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);;
            final SmsFactorSendResponse smsFactorSendResponse = objectMapper.readValue(response.getEntity().getContent(), SmsFactorSendResponse.class);

            if (smsFactorSendResponse.status() != 1) {
                LOGGER.warnf("processResponse: sending SMS failed. Response body of SMSFactor: '%s'", smsFactorSendResponse);
                throw new RuntimeException("processResponse: sending SMS failed");
            }

            LOGGER.debugf("processResponse: SMS send request was successful with status '%d'", smsFactorSendResponse.status());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

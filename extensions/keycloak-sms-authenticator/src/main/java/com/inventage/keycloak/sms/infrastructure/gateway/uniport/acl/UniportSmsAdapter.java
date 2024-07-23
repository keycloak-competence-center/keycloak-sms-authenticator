package com.inventage.keycloak.sms.infrastructure.gateway.uniport.acl;

import com.inventage.keycloak.sms.gateway.SmsService;
import org.apache.http.HttpResponse;
import org.jboss.logging.Logger;

import java.util.UUID;

public class UniportSmsAdapter implements SmsService {

    private static final Logger LOGGER = Logger.getLogger(UniportSmsAdapter.class);

    private final String senderAddress;
    private final UniportSmsTranslator translator;
    private final UniportSmsFacade facade;

    public UniportSmsAdapter(String senderAddress, UniportSmsFacade facade) {
        this.senderAddress = senderAddress;
        this.translator = new UniportSmsTranslator();
        this.facade = facade;
    }

    @Override
    public void send(String phoneNumber, String message) {
        UUID messageId = UUID.randomUUID();

        SmsSendRequest sendRequest = translator.smsRequest(messageId, phoneNumber, message, senderAddress);

        LOGGER.debugf("send: messageId: %s", messageId.toString());
        LOGGER.debugf("send: phone Number: %s", phoneNumber);
        LOGGER.debugf("send: message %s", message);
        LOGGER.debugf("send: senderAddress %s", senderAddress);

        HttpResponse response = facade.post(sendRequest);

        LOGGER.infof("send: Sent message %s and got response %d", messageId.toString(), response.getStatusLine().getStatusCode());
    }

}

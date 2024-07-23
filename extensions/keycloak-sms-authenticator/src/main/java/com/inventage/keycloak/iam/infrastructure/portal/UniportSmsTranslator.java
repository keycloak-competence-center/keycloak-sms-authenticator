package com.inventage.keycloak.iam.infrastructure.portal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

public class UniportSmsTranslator {
    public SmsSendRequest smsRequest(UUID messageId, String phoneNumber, String message, String senderAddress) {
        return new SmsSendRequest(messageId.toString(), new String[]{phoneNumber}, message, senderAddress);
    }

    public String toJson(SmsSendRequest smsSendRequest) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(smsSendRequest);
    }
}

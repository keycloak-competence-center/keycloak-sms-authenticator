package com.inventage.keycloak.iam.infrastructure.portal;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class UniportSmsTranslatorTest {

    private final UniportSmsTranslator translator = new UniportSmsTranslator();
    @Test
    public void toJson() throws JsonProcessingException {
        // given
        final SmsSendRequest smsSendRequest = new SmsSendRequest("messageId", List.of("recipientAddressList").toArray(new String[]{}), "messageContent", "senderAddress");
        // when
        final String json = translator.toJson(smsSendRequest);
        // then
        Assert.assertEquals("{\"messageId\":\"messageId\",\"recipientAddressList\":[\"recipientAddressList\"],\"messageContent\":\"messageContent\",\"senderAddress\":\"senderAddress\"}", json);
    }
}

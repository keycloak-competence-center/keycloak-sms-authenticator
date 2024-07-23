package com.inventage.keycloak.iam.infrastructure.portal;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Arrays;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmsSendRequest {
    private String messageId;
    private String[] recipientAddressList;
    private String messageContent;
    private String senderAddress;

    public SmsSendRequest(String messageId, String[] recipientAddressList, String messageContent, String senderAddress) {
        this.messageId = messageId;
        this.recipientAddressList = recipientAddressList;
        this.messageContent = messageContent;
        this.senderAddress = senderAddress;
    }

    @Override
    public String toString() {
        return "SmsSendRequest{" +
                "messageId='" + messageId + '\'' +
                ", recipientAddressList=" + Arrays.toString(recipientAddressList) +
                ", messageContent='" + messageContent + '\'' +
                ", senderAddress='" + senderAddress + '\'' +
                '}';
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String[] getRecipientAddressList() {
        return recipientAddressList;
    }

    public void setRecipientAddressList(String[] recipientAddressList) {
        this.recipientAddressList = recipientAddressList;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }
}

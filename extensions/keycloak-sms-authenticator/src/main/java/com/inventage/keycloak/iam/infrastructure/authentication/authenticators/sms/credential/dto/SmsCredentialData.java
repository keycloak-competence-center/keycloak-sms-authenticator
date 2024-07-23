package com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SmsCredentialData {

    private final String phoneNumber;

    @JsonCreator
    public SmsCredentialData(@JsonProperty("phoneNumber") String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public String toString() {
        return "SmsCredentialData { " +
                "phoneNumber='" + phoneNumber + '\'' +
                " }";
    }
}

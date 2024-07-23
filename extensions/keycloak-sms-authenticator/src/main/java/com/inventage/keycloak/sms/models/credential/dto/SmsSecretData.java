package com.inventage.keycloak.sms.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class SmsSecretData {

    @JsonCreator
    public SmsSecretData() {
    }


    @Override
    public String toString() {
        return "SmsSecretData {}";
    }
}

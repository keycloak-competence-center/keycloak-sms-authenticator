package com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.credential.dto;

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

package com.inventage.keycloak.sms.models.credential;

public interface SmSChallengeConfiguration {

    int getSmsCodeLength();

    int getSmsCodeTtl();

    String getSmsCodeCharacters();

}

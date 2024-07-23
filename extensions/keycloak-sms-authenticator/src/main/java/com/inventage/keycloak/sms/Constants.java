package com.inventage.keycloak.sms;

public class Constants {

    // Freemarker template for rendering the "Phone number for SMS" UI
    public static final String ENTER_NUMBER_TEMPLATE_NAME = "sms-config.ftl";

    // Freemarker template for rendering the "SMS Code" UI
    public static final String SMS_CHALLENGE_TEMPLATE_NAME = "login-sms.ftl";

    // Parameter in HTTP response used in login-sms.ftl, SmsRequiredAction.java and SmsAuthenticator.java
    public static final String INPUT_ID_CODE = "code";

}

package com.inventage.keycloak.sms.theme;

import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.Theme;

import java.io.IOException;
import java.util.Locale;

public class SmsTextService {

    private final KeycloakSession session;

    public SmsTextService(KeycloakSession session) {
        this.session = session;
    }

    public String getSmsText(String code, int ttl, Locale locale) throws IOException {
        Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
        String smsAuthText = theme.getMessages(locale).getProperty("smsAuthText");
        return String.format(smsAuthText, code, Math.floorDiv(ttl, 60));
    }
}

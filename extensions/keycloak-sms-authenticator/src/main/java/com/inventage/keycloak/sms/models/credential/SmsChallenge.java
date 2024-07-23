package com.inventage.keycloak.sms.models.credential;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.sessions.AuthenticationSessionModel;

public class SmsChallenge {

    private static final String TTL = "ttl";
    private static final String CODE = "code";

    private final AuthenticationSessionModel authenticationSession;

    public SmsChallenge(AuthenticationSessionModel authenticationSessionModel) {
        this.authenticationSession = authenticationSessionModel;
    }

    public boolean isValid(String enteredCode) throws IllegalStateException {
        String code = authenticationSession.getAuthNote(CODE);

        if (code == null) {
            throw new IllegalStateException("isValid: code not found in authSession.");
        }

        return code.equalsIgnoreCase(enteredCode);
    }

    public boolean isExpired() throws IllegalStateException {
        String ttl = authenticationSession.getAuthNote(TTL);

        if (ttl == null) {
            throw new IllegalStateException("isValid: ttl not found in authSession.");
        }

        return Long.parseLong(ttl) < System.currentTimeMillis();
    }

    public String code(SmSChallengeConfiguration config) {
        final String code = SecretGenerator.getInstance().randomString(config.getSmsCodeLength(), getAllowedCharactersInCode(config.getSmsCodeCharacters()));

        authenticationSession.setAuthNote(CODE, code);
        authenticationSession.setAuthNote(TTL, Long.toString(System.currentTimeMillis() + (config.getSmsCodeTtl() * 1000L)));

        return code;
    }

    private char[] getAllowedCharactersInCode(String allowedCharactersString) {
        if (allowedCharactersString != null && !allowedCharactersString.isBlank() && allowedCharactersString.length() > 1) {
            return allowedCharactersString.toCharArray();
        }
        else {
            return SecretGenerator.ALPHANUM;
        }
    }

}

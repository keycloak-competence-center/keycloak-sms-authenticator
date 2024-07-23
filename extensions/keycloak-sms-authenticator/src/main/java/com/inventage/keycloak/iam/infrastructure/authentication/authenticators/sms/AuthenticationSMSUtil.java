package com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms;

import java.io.IOException;
import java.util.Locale;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;

import com.inventage.keycloak.iam.domain.sms.config.SmsConfig;
import com.inventage.keycloak.iam.domain.sms.gateway.SmsServiceFactory;
import com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.credential.SmsCredentialModel;

public class AuthenticationSMSUtil {

    AuthenticationSessionModel authSession;
    private RealmModel realm;
    private UserModel user;
    private KeycloakSession session;

    public static AuthenticationSMSUtil from(AuthenticationFlowContext context) {
        return new AuthenticationSMSUtil(context);
    }

    public static AuthenticationSMSUtil from(RequiredActionContext context) {
        return new AuthenticationSMSUtil(context);
    }

    protected AuthenticationSMSUtil(AuthenticationFlowContext context) {
        this.authSession = context.getAuthenticationSession();
        this.realm = context.getRealm();
        this.user = context.getUser();
        this.session = context.getSession();
    }
    protected AuthenticationSMSUtil(RequiredActionContext context) {
        this.authSession = context.getAuthenticationSession();
        this.realm = context.getRealm();
        this.user = context.getUser();
        this.session = context.getSession();
    }

    public void sendSms() throws IOException {
        String mobileNumber = getMobileNumber();
        sendSms(mobileNumber);
    }

    public void sendSms(String mobileNumber) throws IOException {
        SmsConfig config = SmsConfig.fromRealm(realm);
        int length = config.getLength();
        int ttl = config.getTtl();
        char[] allowedCharactersInCode = getAllowedCharactersInCode(config);

        String code = SecretGenerator.getInstance().randomString(length, allowedCharactersInCode);

        authSession.setAuthNote("code", code);
        authSession.setAuthNote("ttl", Long.toString(System.currentTimeMillis() + (ttl * 1000L)));


        Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
        Locale locale = session.getContext().resolveLocale(user);
        String smsAuthText = theme.getMessages(locale).getProperty("smsAuthText");
        String smsText = String.format(smsAuthText, code, Math.floorDiv(ttl, 60));

        SmsServiceFactory.get(config, session.getProvider(HttpClientProvider.class).getHttpClient()).send(mobileNumber, smsText);
    }

    public boolean isValid(String enteredCode) throws IllegalStateException {
        String code = authSession.getAuthNote("code");

        if (code == null) {
            throw new IllegalStateException("isValid: code not found in authSession.");
        }

        return code.equalsIgnoreCase(enteredCode);
    }

    public boolean isExpired() throws IllegalStateException {
        String ttl = authSession.getAuthNote("ttl");

        if (ttl == null) {
            throw new IllegalStateException("isValid: ttl not found in authSession.");
        }

        return Long.parseLong(ttl) < System.currentTimeMillis();
    }

    private String getMobileNumber() {
        CredentialModel credentialModel = user.credentialManager()
                .getStoredCredentialsByTypeStream(SmsCredentialModel.TYPE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no mobile number configured for user"));

        SmsCredentialModel smsCredentialModel = SmsCredentialModel.createFromCredentialModel(credentialModel);
        return smsCredentialModel.getSmsCredentialData().getPhoneNumber();
    }

    private char[] getAllowedCharactersInCode(SmsConfig config) {
        String allowedCharactersString = config.getAllowedCharactersInCode();
        if (allowedCharactersString != null && !allowedCharactersString.isBlank() && allowedCharactersString.length() > 1) {
            return allowedCharactersString.toCharArray();
        }
        else {
            return SecretGenerator.ALPHANUM;
        }
    }
}

package com.inventage.keycloak.sms.authentication.authenticators;

import com.inventage.keycloak.sms.authentication.SmsCodeConfiguration;
import com.inventage.keycloak.sms.authentication.requireactions.SmsRequiredAction;
import com.inventage.keycloak.sms.gateway.SmsServiceProvider;
import com.inventage.keycloak.sms.models.credential.SmsChallenge;
import com.inventage.keycloak.sms.models.credential.SmsCredentialModel;
import com.inventage.keycloak.sms.theme.SmsTextService;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.*;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.io.IOException;
import java.util.Objects;

import static com.inventage.keycloak.sms.Constants.INPUT_ID_CODE;
import static com.inventage.keycloak.sms.Constants.SMS_CHALLENGE_TEMPLATE_NAME;

/**
 * @author Geraldine von Roten
 */
public class SmsAuthenticator implements Authenticator {

    public static final String SMS_AUTH_CODE_INVALID = "smsAuthCodeInvalid";

    private static final String MOBILE_NUMBER_ATTRIBUTE = "mobileNumber";
    private static final Logger LOGGER = Logger.getLogger(SmsAuthenticator.class);
    private static final String SMS_SENT_NOTE = "smsSent";
    private static final String SMS_NOTE_VALUE = "true";
    private static final String REALM_ATTRIBUTE = "realm";
    private static final String SHOW_PHONE_NUMBER_ATTRIBUTE = "showPhoneNumber";
    private static final String SMS_RESENT_INFO_ATTRIBUTE = "smsResent";

    private final SmsTextService smsTextService;

    public SmsAuthenticator(SmsTextService smsTextService) {
        this.smsTextService = smsTextService;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        try {
            context.challenge(sendSmsChallengeAndAskUserForCode(context));
        } catch (Exception e) {
            context.failureChallenge(
                    AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().setError("smsAuthSmsNotSent", e.getMessage())
                            .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    private Response sendSmsChallengeAndAskUserForCode(AuthenticationFlowContext context) throws IOException {
        final String mobileNumber = getMobileNumber(context.getUser());
        final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getAuthenticatorConfig().getConfig());
        sendSmsChallenge(context.getUser(), mobileNumber, smsCodeConfiguration, context.getAuthenticationSession(), context.getSession());

        final String smsResent = context.getAuthenticationSession().getAuthNote(SMS_SENT_NOTE);
        if (Objects.equals(smsResent, SMS_NOTE_VALUE)) {
            return context.form()
                    .setAttribute(REALM_ATTRIBUTE, context.getRealm())
                    .setAttribute(SHOW_PHONE_NUMBER_ATTRIBUTE, smsCodeConfiguration.getShowPhoneNumber(context.getAuthenticatorConfig()))
                    .setAttribute(MOBILE_NUMBER_ATTRIBUTE, mobileNumber)
                    .setAttribute(SMS_RESENT_INFO_ATTRIBUTE, true)
                    .createForm(SMS_CHALLENGE_TEMPLATE_NAME);
        }

        context.getAuthenticationSession().setAuthNote(SMS_SENT_NOTE, SMS_NOTE_VALUE);
        return context.form()
                .setAttribute(REALM_ATTRIBUTE, context.getRealm())
                .setAttribute(SHOW_PHONE_NUMBER_ATTRIBUTE, smsCodeConfiguration.getShowPhoneNumber(context.getAuthenticatorConfig()))
                .setAttribute(MOBILE_NUMBER_ATTRIBUTE, mobileNumber)
                .createForm(SMS_CHALLENGE_TEMPLATE_NAME);
    }

    private void sendSmsChallenge(UserModel user, String mobileNumber, SmsCodeConfiguration smsCodeConfiguration, AuthenticationSessionModel authenticationSession, KeycloakSession session) throws IOException {
        final SmsServiceProvider smsServiceProvider = session.getProvider(SmsServiceProvider.class, smsCodeConfiguration.getSmsServiceProviderId());
        if (smsServiceProvider == null) {
            final IllegalStateException exception = new IllegalStateException("Sms Service Provider is null");
            LOGGER.warnf(exception, "sendSmsChallenge: SMS couldn't be sent, because SmsServiceProvider '%s' not found!", smsCodeConfiguration.getSmsServiceProviderId());
            throw exception;
        }

        String code = new SmsChallenge(authenticationSession).code(smsCodeConfiguration);
        String smsText = smsTextService.getSmsText(code, smsCodeConfiguration.getSmsCodeTtl(), session.getContext().resolveLocale(user));
        smsServiceProvider.getSmsService().send(mobileNumber, smsText);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        String enteredCode = context.getHttpRequest().getDecodedFormParameters().getFirst(INPUT_ID_CODE);
        validate(enteredCode, context);
    }

    private void validate(String enteredCode, AuthenticationFlowContext context) {
        final SmsChallenge smsChallenge = new SmsChallenge(context.getAuthenticationSession());
        if (smsChallenge.isValid(enteredCode)) {
            if (smsChallenge.isExpired()) {
                // expired
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
                        context.form().setError("smsAuthCodeExpired").createErrorPage(Response.Status.BAD_REQUEST));
            } else {
                // valid
                context.success();
            }
        } else {
            // invalid
            AuthenticationExecutionModel execution = context.getExecution();
            final String mobileNumber = getMobileNumber(context.getUser());
            final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getAuthenticatorConfig().getConfig());
            if (execution.isRequired()) {
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                        context.form()
                                .setAttribute(REALM_ATTRIBUTE, context.getRealm())
                                .setAttribute(SHOW_PHONE_NUMBER_ATTRIBUTE, smsCodeConfiguration.getShowPhoneNumber(context.getAuthenticatorConfig()))
                                .setAttribute(MOBILE_NUMBER_ATTRIBUTE, mobileNumber)
                                .setError(SMS_AUTH_CODE_INVALID)
                                .setAttribute(SMS_AUTH_CODE_INVALID, true)
                                .createForm(SMS_CHALLENGE_TEMPLATE_NAME));
            } else if (execution.isConditional() || execution.isAlternative()) {
                context.attempted();
            }
        }
    }

    private String getMobileNumber(UserModel user) {
        CredentialModel credentialModel = user.credentialManager()
                .getStoredCredentialsByTypeStream(SmsCredentialModel.TYPE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no mobile number configured for user"));

        SmsCredentialModel smsCredentialModel = SmsCredentialModel.createFromCredentialModel(credentialModel);
        return smsCredentialModel.getSmsCredentialData().getPhoneNumber();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user.credentialManager()
                .getStoredCredentialsByTypeStream(SmsCredentialModel.TYPE).findAny().isPresent();
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction(SmsRequiredAction.PROVIDER_ID);
    }

    @Override
    public void close() {
    }
}
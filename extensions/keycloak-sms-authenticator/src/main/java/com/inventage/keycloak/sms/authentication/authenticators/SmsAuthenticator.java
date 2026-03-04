package com.inventage.keycloak.sms.authentication.authenticators;

import com.inventage.keycloak.sms.authentication.SmsChallengeHelper;
import com.inventage.keycloak.sms.authentication.SmsCodeConfiguration;
import com.inventage.keycloak.sms.authentication.requireactions.SmsRequiredAction;
import com.inventage.keycloak.sms.gateway.SmsRateLimitedException;
import com.inventage.keycloak.sms.models.credential.SmsChallenge;
import com.inventage.keycloak.sms.models.credential.SmsCredentialModel;
import com.inventage.keycloak.sms.theme.SmsTextService;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;

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
            final String mobileNumber = SmsChallengeHelper.getMobileNumber(context.getUser())
                    .orElseThrow(() -> new IllegalStateException("no mobile number configured for user"));
            final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getAuthenticatorConfig().getConfig());

            final String smsSent = context.getAuthenticationSession().getAuthNote(SMS_SENT_NOTE);
            String smsError = null;
            if (smsSent == null) {
                try {
                    SmsChallengeHelper.sendSmsChallenge(mobileNumber, smsCodeConfiguration, context.getAuthenticationSession(), context.getSession(), context.getUser(), smsTextService);
                }
                catch (SmsRateLimitedException e) {
                    LOGGER.warn("authenticate: SMS sending was rate limited", e);
                    smsError = "smsAuthSmsRateLimited";
                }
                context.getAuthenticationSession().setAuthNote(SMS_SENT_NOTE, SMS_NOTE_VALUE);
            }
            final LoginFormsProvider form = context.form()
                    .setAttribute(REALM_ATTRIBUTE, context.getRealm())
                    .setAttribute(SHOW_PHONE_NUMBER_ATTRIBUTE, smsCodeConfiguration.getShowPhoneNumber(context.getAuthenticatorConfig()))
                    .setAttribute(MOBILE_NUMBER_ATTRIBUTE, mobileNumber);
            if (smsError != null) {
                form.setError(smsError);
            }
            context.challenge(form.createForm(SMS_CHALLENGE_TEMPLATE_NAME));
        }
        catch (Exception e) {
            context.failureChallenge(
                    AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().setError("smsAuthSmsNotSent", e.getMessage())
                            .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        final MultivaluedMap<String, String> formParams = context.getHttpRequest().getDecodedFormParameters();
        if (SmsChallengeHelper.isResendSms(formParams)) {
            final String mobileNumber = SmsChallengeHelper.getMobileNumber(context.getUser())
                    .orElseThrow(() -> new IllegalStateException("no mobile number configured for user"));
            final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getAuthenticatorConfig().getConfig());
            try {
                SmsChallengeHelper.sendSmsChallenge(mobileNumber, smsCodeConfiguration, context.getAuthenticationSession(), context.getSession(), context.getUser(), smsTextService);
                context.challenge(context.form()
                        .setAttribute(REALM_ATTRIBUTE, context.getRealm())
                        .setAttribute(SHOW_PHONE_NUMBER_ATTRIBUTE, smsCodeConfiguration.getShowPhoneNumber(context.getAuthenticatorConfig()))
                        .setAttribute(MOBILE_NUMBER_ATTRIBUTE, mobileNumber)
                        .setAttribute(SMS_RESENT_INFO_ATTRIBUTE, true)
                        .createForm(SMS_CHALLENGE_TEMPLATE_NAME));
            }
            catch (SmsRateLimitedException e) {
                LOGGER.warn("action: SMS resend was rate limited", e);
                context.challenge(context.form()
                        .setAttribute(REALM_ATTRIBUTE, context.getRealm())
                        .setAttribute(SHOW_PHONE_NUMBER_ATTRIBUTE, smsCodeConfiguration.getShowPhoneNumber(context.getAuthenticatorConfig()))
                        .setAttribute(MOBILE_NUMBER_ATTRIBUTE, mobileNumber)
                        .setError("smsAuthSmsRateLimited")
                        .createForm(SMS_CHALLENGE_TEMPLATE_NAME));
            }
            catch (Exception e) {
                context.failureChallenge(
                        AuthenticationFlowError.INTERNAL_ERROR,
                        context.form().setError("smsAuthSmsNotSent", e.getMessage())
                                .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
            }
            return;
        }

        final String enteredCode = context.getHttpRequest().getDecodedFormParameters().getFirst(INPUT_ID_CODE);
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
            final String mobileNumber = SmsChallengeHelper.getMobileNumber(context.getUser())
                    .orElseThrow(() -> new IllegalStateException("no mobile number configured for user"));
            final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getAuthenticatorConfig().getConfig());
            if (execution.isRequired()) {
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                        context.form()
                                .setAttribute(REALM_ATTRIBUTE, context.getRealm())
                                .setAttribute(SHOW_PHONE_NUMBER_ATTRIBUTE, smsCodeConfiguration.getShowPhoneNumber(context.getAuthenticatorConfig()))
                                .setAttribute(MOBILE_NUMBER_ATTRIBUTE, mobileNumber)
                                .addError(new FormMessage(INPUT_ID_CODE, SMS_AUTH_CODE_INVALID))
                                .createForm(SMS_CHALLENGE_TEMPLATE_NAME));
            } else if (execution.isConditional() || execution.isAlternative()) {
                context.attempted();
            }
        }
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
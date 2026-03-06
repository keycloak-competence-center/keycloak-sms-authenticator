package com.inventage.keycloak.sms.authentication.requireactions;

import com.inventage.keycloak.sms.authentication.SmsChallengeHelper;
import com.inventage.keycloak.sms.authentication.SmsCodeConfiguration;
import com.inventage.keycloak.sms.authentication.SmsCodeValidationResult;
import com.inventage.keycloak.sms.gateway.SmsRateLimitedException;
import com.inventage.keycloak.sms.theme.SmsTextService;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.utils.FormMessage;

import static com.inventage.keycloak.sms.Constants.INPUT_ID_CODE;
import static com.inventage.keycloak.sms.Constants.SMS_CHALLENGE_TEMPLATE_NAME;
import static com.inventage.keycloak.sms.authentication.SmsCodeValidationResult.EXPIRED;
import static com.inventage.keycloak.sms.authentication.SmsCodeValidationResult.VALID;

/**
 * Required action that verifies an existing SMS credential without showing the phone number entry screen.
 * Designed for onboarding flows where the phone number is pre-provisioned by an admin.
 * <p>
 * Brute force protection is handled manually since the required action framework does not
 * count failures automatically (unlike the authenticator framework's {@code failureChallenge}).
 */
public class SmsVerifyAction implements RequiredActionProvider {

    private static final Logger LOGGER = Logger.getLogger(SmsVerifyAction.class);

    public static final String PROVIDER_ID = "sms-verify";

    private static final String SMS_SENT_NOTE = "smsSent";
    private static final String SMS_NOTE_VALUE = "true";

    private final SmsTextService smsTextService;

    public SmsVerifyAction(SmsTextService smsTextService) {
        this.smsTextService = smsTextService;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        // NOP
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        final String bruteForceError = SmsChallengeHelper.getDisabledByBruteForceEventError(context);
        if (bruteForceError != null) {
            context.getEvent().user(context.getUser()).error(bruteForceError);
            buildChallengeForm(context, SmsChallengeHelper.disabledByBruteForceError(bruteForceError), null, false);
            return;
        }

        final String mobileNumber = SmsChallengeHelper.getMobileNumber(context.getUser()).orElse(null);
        if (mobileNumber == null) {
            LOGGER.errorf("requiredActionChallenge: no SMS credential found for user '%s'", context.getUser().getUsername());
            context.challenge(context.form()
                    .setError("smsAuthSmsNotSent", "No phone number configured")
                    .createErrorPage(Response.Status.BAD_REQUEST));
            return;
        }

        final String smsSent = context.getAuthenticationSession().getAuthNote(SMS_SENT_NOTE);
        String smsError = null;
        if (smsSent == null) {
            try {
                final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getConfig().getConfig());
                SmsChallengeHelper.sendSmsChallenge(mobileNumber, smsCodeConfiguration, context.getAuthenticationSession(), context.getSession(), context.getUser(), smsTextService);
            }
            catch (SmsRateLimitedException e) {
                LOGGER.warn("requiredActionChallenge: SMS sending was rate limited", e);
                smsError = "smsAuthSmsRateLimited";
            }
            catch (Exception e) {
                LOGGER.error("error while sending sms", e);
                context.challenge(context.form()
                        .setError("smsAuthSmsNotSent", e.getMessage())
                        .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
                return;
            }
            context.getAuthenticationSession().setAuthNote(SMS_SENT_NOTE, SMS_NOTE_VALUE);
        }

        buildChallengeForm(context, null, smsError, false);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        final String bruteForceError = SmsChallengeHelper.getDisabledByBruteForceEventError(context);
        if (bruteForceError != null) {
            context.getEvent().user(context.getUser()).error(bruteForceError);
            buildChallengeForm(context, SmsChallengeHelper.disabledByBruteForceError(bruteForceError), null, false);
            return;
        }

        if (SmsChallengeHelper.isResendSms(context.getHttpRequest().getDecodedFormParameters())) {
            final String mobileNumber = SmsChallengeHelper.getMobileNumber(context.getUser()).orElse(null);
            try {
                final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getConfig().getConfig());
                SmsChallengeHelper.sendSmsChallenge(mobileNumber, smsCodeConfiguration, context.getAuthenticationSession(), context.getSession(), context.getUser(), smsTextService);
                buildChallengeForm(context, null, null, true);
            }
            catch (SmsRateLimitedException e) {
                LOGGER.warn("processAction: SMS resend was rate limited", e);
                buildChallengeForm(context, null, "smsAuthSmsRateLimited", false);
            }
            catch (Exception e) {
                LOGGER.error("error while resending sms", e);
                buildChallengeForm(context, null, "smsAuthSmsNotSent", false);
            }
        }
        else {
            final String code = context.getHttpRequest().getDecodedFormParameters().getFirst(INPUT_ID_CODE);
            final SmsCodeValidationResult result = SmsChallengeHelper.validateCode(code, context.getAuthenticationSession());
            if (result != VALID) {
                if (result != EXPIRED) {
                    context.getEvent().user(context.getUser()).error(Errors.INVALID_USER_CREDENTIALS);
                    SmsChallengeHelper.registerFailedAttempt(context);
                }
                buildChallengeForm(context, result.messageKey(), null, false);
            }
            else {
                context.success();
            }
        }
    }

    private void buildChallengeForm(RequiredActionContext context, String fieldError, String globalError, boolean smsResent) {
        final LoginFormsProvider form = context.form();
        if (fieldError != null) {
            form.addError(new FormMessage(INPUT_ID_CODE, fieldError));
        }
        if (globalError != null) {
            form.setError(globalError);
        }
        if (smsResent) {
            form.setAttribute("smsResent", true);
        }

        final String mobileNumber = SmsChallengeHelper.getMobileNumber(context.getUser()).orElse(null);
        final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getConfig().getConfig());
        form.setAttribute("showPhoneNumber", smsCodeConfiguration.getShowPhoneNumber(context.getRealm().getAuthenticatorConfigByAlias(PROVIDER_ID)));
        form.setAttribute("mobileNumber", mobileNumber);
        final Response challenge = form.createForm(SMS_CHALLENGE_TEMPLATE_NAME);
        context.challenge(challenge);
    }

    @Override
    public void close() {
    }
}

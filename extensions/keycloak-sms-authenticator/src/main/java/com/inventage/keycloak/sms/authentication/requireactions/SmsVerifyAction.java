package com.inventage.keycloak.sms.authentication.requireactions;

import com.inventage.keycloak.sms.authentication.SmsCodeConfiguration;
import com.inventage.keycloak.sms.gateway.SmsRateLimitedException;
import com.inventage.keycloak.sms.gateway.SmsServiceProvider;
import com.inventage.keycloak.sms.models.credential.SmsChallenge;
import com.inventage.keycloak.sms.models.credential.SmsCredentialModel;
import com.inventage.keycloak.sms.theme.SmsTextService;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static com.inventage.keycloak.sms.Constants.*;

/**
 * Required action that verifies an existing SMS credential without showing the phone number entry screen.
 * Designed for onboarding flows where the phone number is pre-provisioned by an admin.
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
        final String mobileNumber = getMobileNumber(context.getUser());
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
                sendSmsChallenge(mobileNumber, context.getConfig().getConfig(), context.getSession(), context);
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
        if (isResendSms(context)) {
            final String mobileNumber = getMobileNumber(context.getUser());
            try {
                sendSmsChallenge(mobileNumber, context.getConfig().getConfig(), context.getSession(), context);
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
            final Optional<String> error = validateCode(code, context);
            if (error.isPresent()) {
                buildChallengeForm(context, error.get(), null, false);
            }
            else {
                context.success();
            }
        }
    }

    private void sendSmsChallenge(String mobileNumber, Map<String, String> config, KeycloakSession session, RequiredActionContext context) throws IOException {
        final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(config);
        final SmsServiceProvider smsServiceProvider = session.getProvider(SmsServiceProvider.class, smsCodeConfiguration.getSmsServiceProviderId());
        if (smsServiceProvider == null) {
            LOGGER.warnf("sendSmsChallenge: SMS couldn't be sent, because SmsServiceProvider '%s' not found!", smsCodeConfiguration.getSmsServiceProviderId());
        }

        final String code = new SmsChallenge(context.getAuthenticationSession()).code(smsCodeConfiguration);
        final String smsText = smsTextService.getSmsText(code, smsCodeConfiguration.getSmsCodeTtl(), session.getContext().resolveLocale(context.getUser()));
        smsServiceProvider.getSmsService().send(mobileNumber, smsText);
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

        final String mobileNumber = getMobileNumber(context.getUser());
        final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getConfig().getConfig());
        form.setAttribute("showPhoneNumber", smsCodeConfiguration.getShowPhoneNumber(context.getRealm().getAuthenticatorConfigByAlias(PROVIDER_ID)));
        form.setAttribute("mobileNumber", mobileNumber);
        final Response challenge = form.createForm(SMS_CHALLENGE_TEMPLATE_NAME);
        context.challenge(challenge);
    }

    private boolean isResendSms(RequiredActionContext context) {
        return context.getHttpRequest().getDecodedFormParameters().getFirst(RESEND_SMS) != null;
    }

    private Optional<String> validateCode(String code, RequiredActionContext context) {
        if (code == null || code.isBlank()) {
            return Optional.of("sms.code.error.empty");
        }
        if (!new SmsChallenge(context.getAuthenticationSession()).isValid(code)) {
            return Optional.of("sms.code.error.wrong");
        }
        return Optional.empty();
    }

    private String getMobileNumber(UserModel user) {
        return user.credentialManager()
                .getStoredCredentialsByTypeStream(SmsCredentialModel.TYPE)
                .findFirst()
                .map(credentialModel -> SmsCredentialModel.createFromCredentialModel(credentialModel).getSmsCredentialData().getPhoneNumber())
                .orElse(null);
    }

    @Override
    public void close() {
    }
}

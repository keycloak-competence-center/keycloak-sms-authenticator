package com.inventage.keycloak.sms.authentication.requireactions;

import com.inventage.keycloak.sms.authentication.PhoneNumberUtils;
import com.inventage.keycloak.sms.authentication.SmsChallengeHelper;
import com.inventage.keycloak.sms.authentication.SmsCodeConfiguration;
import com.inventage.keycloak.sms.authentication.SmsCodeValidationResult;
import com.inventage.keycloak.sms.credential.SmsCredentialProvider;
import com.inventage.keycloak.sms.credential.SmsCredentialProviderFactory;
import com.inventage.keycloak.sms.gateway.SmsRateLimitedException;
import com.inventage.keycloak.sms.models.credential.SmsCredentialModel;
import com.inventage.keycloak.sms.theme.SmsTextService;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.logging.Logger;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.net.URI;
import java.util.Optional;

import static com.inventage.keycloak.sms.Constants.ENTER_NUMBER_TEMPLATE_NAME;
import static com.inventage.keycloak.sms.Constants.INPUT_ID_CODE;
import static com.inventage.keycloak.sms.Constants.SMS_CHALLENGE_TEMPLATE_NAME;
import static com.inventage.keycloak.sms.authentication.SmsCodeValidationResult.VALID;


/**
 * Required action for registering and verifying a phone number for SMS authentication.
 */
public class SmsRequiredAction implements RequiredActionProvider, CredentialRegistrator {

    private static final Logger LOGGER = Logger.getLogger(SmsRequiredAction.class);

    public static final String PROVIDER_ID = "sms-config";
    public static final String ENTERED_NUMBER_KEY = "entered-number";

    private static final String STATE_KEY= "number-required-action-provider-state";

    private static final String RESET_NUMBER_QUERY_KEY = "resetNumber";
    private final SmsTextService smsTextService;

    private RequiredActionContext context;

    public SmsRequiredAction(SmsTextService smsTextService) {
        this.smsTextService = smsTextService;
    }

    @Override
    public String getCredentialType(KeycloakSession session, AuthenticationSessionModel authenticationSession) {
        return SmsCredentialModel.TYPE;
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        //NOP
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        this.context = context;

        final String bruteForceError = SmsChallengeHelper.getDisabledByBruteForceEventError(context);
        if (bruteForceError != null) {
            context.getEvent().user(context.getUser()).error(bruteForceError);
            final State state = getStateOrDefault();
            if (State.CHALLENGE.equals(state)) {
                showChallengeScreenWithFieldError(SmsChallengeHelper.disabledByBruteForceError(bruteForceError));
            }
            else {
                showEnterNumberScreenWithGlobalError(SmsChallengeHelper.disabledByBruteForceError(bruteForceError), null);
            }
            return;
        }

        final String enteredNumber = context.getAuthenticationSession().getAuthNote(ENTERED_NUMBER_KEY);

        if (enteredNumber == null) {
            setState(State.ENTERING_NUMBER);
            showEnterNumberScreen();
        }
        else {
            final State state = getStateOrDefault();
            if (State.CHALLENGE.equals(state)) {
                showChallengeScreen();
            }
            else {
                try {
                    final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getConfig().getConfig());
                    SmsChallengeHelper.sendSmsChallenge(enteredNumber, smsCodeConfiguration, context.getAuthenticationSession(), context.getSession(), context.getUser(), smsTextService);
                    setState(State.CHALLENGE);
                    showChallengeScreen();
                }
                catch (SmsRateLimitedException e) {
                    LOGGER.warn("requiredActionChallenge: SMS sending was rate limited", e);
                    setState(State.CHALLENGE);
                    showChallengeScreen(Optional.of("smsAuthSmsRateLimited"));
                }
                catch (Exception e) {
                    LOGGER.error("error while sending sms", e);
                    showEnterNumberScreenWithGlobalError("smsPhoneNumberErrorSending", enteredNumber);
                }
            }
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        this.context = context;
        if (isResetPhoneNumber()) {
            setState(State.ENTERING_NUMBER);
            context.getAuthenticationSession().removeAuthNote(ENTERED_NUMBER_KEY);
            showEnterNumberScreen();
        }
        else if (SmsChallengeHelper.isResendSms(context.getHttpRequest().getDecodedFormParameters())) {
            try {
                final String enteredNumber = context.getAuthenticationSession().getAuthNote(ENTERED_NUMBER_KEY);
                final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getConfig().getConfig());
                SmsChallengeHelper.sendSmsChallenge(enteredNumber, smsCodeConfiguration, context.getAuthenticationSession(), context.getSession(), context.getUser(), smsTextService);
                showChallengeScreen(Optional.empty(), true);
            }
            catch (SmsRateLimitedException e) {
                LOGGER.warn("processAction: SMS resend was rate limited", e);
                showChallengeScreen(Optional.of("smsAuthSmsRateLimited"));
            }
            catch (Exception e) {
                LOGGER.error("error while resending sms", e);
                showChallengeScreen(Optional.of("smsAuthSmsNotSent"));
            }
        }
        else {
            final State state = getState();
            if (State.ENTERING_NUMBER.equals(state)) {
                enteredNumber();
            } else if (State.CHALLENGE.equals(state)) {
                challenge();
            }
        }
    }

    private boolean isResetPhoneNumber() {
        MultivaluedMap<String, String> queryParameters = context.getSession().getContext().getUri().getQueryParameters();
        return queryParameters.containsKey(RESET_NUMBER_QUERY_KEY);
    }

    private void challenge() {
        String code = context.getHttpRequest().getDecodedFormParameters().getFirst(INPUT_ID_CODE);
        final SmsCodeValidationResult result = SmsChallengeHelper.validateCode(code, context.getAuthenticationSession());
        if (result != VALID) {
            showChallengeScreenWithFieldError(result.messageKey());
        }
        else {
            SmsCredentialProvider credentialProvider =
                    (SmsCredentialProvider) context.getSession().getProvider(CredentialProvider.class, SmsCredentialProviderFactory.PROVIDER_ID);
            String enteredNumber = context.getAuthenticationSession().getAuthNote(ENTERED_NUMBER_KEY);
            SmsCredentialModel credentialModel = SmsCredentialModel.create(enteredNumber);
            credentialProvider.createCredential(context.getRealm(), context.getUser(), credentialModel);
            context.success();
        }
    }

    private void enteredNumber() {
        String phoneNumber = context.getHttpRequest().getDecodedFormParameters().getFirst("phone-number");
        phoneNumber = PhoneNumberUtils.clean(phoneNumber);

        Optional<String> error = validatePhoneNumber(phoneNumber);
        if (error.isPresent()) {
            showEnterNumberScreenWithFieldError(error.get(), phoneNumber);
        }
        else {
            try {
                final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getConfig().getConfig());
                SmsChallengeHelper.sendSmsChallenge(phoneNumber, smsCodeConfiguration, context.getAuthenticationSession(), context.getSession(), context.getUser(), smsTextService);
                setState(State.CHALLENGE);
                context.getAuthenticationSession().setAuthNote(ENTERED_NUMBER_KEY, phoneNumber);
                showChallengeScreen();
            }
            catch (SmsRateLimitedException e) {
                LOGGER.warn("enteredNumber: SMS sending was rate limited", e);
                setState(State.CHALLENGE);
                context.getAuthenticationSession().setAuthNote(ENTERED_NUMBER_KEY, phoneNumber);
                showChallengeScreen(Optional.of("smsAuthSmsRateLimited"));
            }
            catch (Exception e) {
                LOGGER.error("error while sending sms", e);
                showEnterNumberScreenWithGlobalError("smsPhoneNumberErrorSending", phoneNumber);
            }
        }
    }

    private void showEnterNumberScreen() {
        buildEnterNumberForm(null, null, null);
    }

    private void showEnterNumberScreenWithFieldError(String fieldError, String phoneNumber) {
        buildEnterNumberForm(fieldError, null, phoneNumber);
    }

    private void showEnterNumberScreenWithGlobalError(String globalError, String phoneNumber) {
        buildEnterNumberForm(null, globalError, phoneNumber);
    }

    private void buildEnterNumberForm(String fieldError, String globalError, String phoneNumber) {
        final LoginFormsProvider form = context.form();
        if (fieldError != null) {
            form.addError(new FormMessage("phone-number", fieldError));
        }
        if (globalError != null) {
            form.setError(globalError);
        }
        form.setAttribute("username", context.getUser().getUsername());
        if (phoneNumber != null) {
            form.setAttribute("phoneNumber", phoneNumber);
        }
        final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getConfig().getConfig());
        final String hint = smsCodeConfiguration.getPhoneNumberValidationHint();
        if (hint != null && !hint.isEmpty()) {
            form.setAttribute("phoneNumberHint", hint);
        }
        final Response challenge = form.createForm(ENTER_NUMBER_TEMPLATE_NAME);
        context.challenge(challenge);
    }

    private void showChallengeScreen() {
        buildChallengeForm(null, null, false);
    }

    private void showChallengeScreen(Optional<String> globalError) {
        buildChallengeForm(null, globalError.orElse(null), false);
    }

    private void showChallengeScreen(Optional<String> globalError, boolean smsResent) {
        buildChallengeForm(null, globalError.orElse(null), smsResent);
    }

    private void showChallengeScreenWithFieldError(String fieldError) {
        buildChallengeForm(fieldError, null, false);
    }

    private void buildChallengeForm(String fieldError, String globalError, boolean smsResent) {
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

        addResetPhoneNumberActionUrl(form);
        final String mobileNumber = context.getAuthenticationSession().getAuthNote(ENTERED_NUMBER_KEY);
        final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getConfig().getConfig());
        form.setAttribute("showPhoneNumber", smsCodeConfiguration.getShowPhoneNumber(context.getRealm().getAuthenticatorConfigByAlias(PROVIDER_ID)));
        form.setAttribute("mobileNumber", mobileNumber);
        final Response challenge = form.createForm(SMS_CHALLENGE_TEMPLATE_NAME);
        context.challenge(challenge);
    }

    private void addResetPhoneNumberActionUrl(LoginFormsProvider form) {
        URI originalActionUri = context.getActionUrl();
        URI actionUri = UriBuilder.fromUri(originalActionUri).queryParam(RESET_NUMBER_QUERY_KEY, Boolean.TRUE).build();
        form.setAttribute("resetPhoneNumberUri", actionUri);
    }

    private Optional<String> validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return Optional.of("smsPhoneNumberErrorEmpty");
        }

        final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getConfig().getConfig());
        final String regex = smsCodeConfiguration.getPhoneNumberValidationRegex();
        if (regex != null && !regex.isEmpty() && !phoneNumber.matches(regex)) {
            return Optional.of("smsPhoneNumberErrorInvalidFormat");
        }

        return Optional.empty();
    }

    @Override
    public void close() {
    }

    private State getState() {
        return State.valueOf(context.getAuthenticationSession().getAuthNote(STATE_KEY));
    }

    private State getStateOrDefault() {
        final String stateNote = context.getAuthenticationSession().getAuthNote(STATE_KEY);
        if (stateNote == null) {
            return State.ENTERING_NUMBER;
        }
        return State.valueOf(stateNote);
    }

    private void setState(State state) {
        context.getAuthenticationSession().setAuthNote(STATE_KEY, state.name());
    }

    private enum State {
        ENTERING_NUMBER,
        CHALLENGE
    }
}

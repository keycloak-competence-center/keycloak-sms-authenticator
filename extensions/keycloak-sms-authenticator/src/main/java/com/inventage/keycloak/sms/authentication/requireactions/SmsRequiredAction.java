package com.inventage.keycloak.sms.authentication.requireactions;

import com.inventage.keycloak.sms.Constants;
import com.inventage.keycloak.sms.authentication.PhoneNumberUtils;
import com.inventage.keycloak.sms.authentication.SmsCodeConfiguration;
import com.inventage.keycloak.sms.credential.SmsCredentialProvider;
import com.inventage.keycloak.sms.credential.SmsCredentialProviderFactory;
import com.inventage.keycloak.sms.gateway.SmsRateLimitedException;
import com.inventage.keycloak.sms.gateway.SmsServiceProvider;
import com.inventage.keycloak.sms.models.credential.SmsChallenge;
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
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static com.inventage.keycloak.sms.Constants.*;
import static com.inventage.keycloak.sms.authentication.authenticators.SmsAuthenticator.SMS_AUTH_CODE_INVALID;


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
                    sendSmsChallenge(enteredNumber, context.getConfig().getConfig(), context.getSession());
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
                    showEnterNumberScreen(Optional.of("sms.phoneNumber.error.sending"));
                }
            }
        }
    }

    private void sendSmsChallenge(String mobileNumber, Map<String, String> config, KeycloakSession session) throws IOException {
        final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(config);
        final SmsServiceProvider smsServiceProvider = session.getProvider(SmsServiceProvider.class, smsCodeConfiguration.getSmsServiceProviderId());
        if (smsServiceProvider == null) {
            LOGGER.warnf("sendSmsChallenge: SMS couldn't be send, because SmsServiceProvider '%s' not found!", smsCodeConfiguration.getSmsServiceProviderId());
        }

        String code = new SmsChallenge(context.getAuthenticationSession()).code(smsCodeConfiguration);
        String smsText = smsTextService.getSmsText(code, smsCodeConfiguration.getSmsCodeTtl(), session.getContext().resolveLocale(context.getUser()));
        smsServiceProvider.getSmsService().send(mobileNumber, smsText);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        this.context = context;
        if (isResetPhoneNumber()) {
            setState(State.ENTERING_NUMBER);
            context.getAuthenticationSession().removeAuthNote(ENTERED_NUMBER_KEY);
            showEnterNumberScreen();
        }
        else if (isResendSms()) {
            try {
                final String enteredNumber = context.getAuthenticationSession().getAuthNote(ENTERED_NUMBER_KEY);
                sendSmsChallenge(enteredNumber, context.getConfig().getConfig(), context.getSession());
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

    private boolean isResendSms() {
        return context.getHttpRequest().getDecodedFormParameters().getFirst(Constants.RESEND_SMS) != null;
    }

    private void challenge() {
        String code = context.getHttpRequest().getDecodedFormParameters().getFirst(INPUT_ID_CODE);
        Optional<String> error = validateCode(code);
        if (error.isPresent()) {
            showChallengeScreen(error);
            //TODO send code again???
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
            showEnterNumberScreen(error);
        }
        else {
            try {
                sendSmsChallenge(phoneNumber, context.getConfig().getConfig(), context.getSession());
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
                showEnterNumberScreen(Optional.of("sms.phoneNumber.error.sending"));
            }
        }
    }

    private void showEnterNumberScreen() {
        showEnterNumberScreen(Optional.empty());
    }

    private void showEnterNumberScreen(Optional<String> error) {
        final LoginFormsProvider form = context.form();
        if (error.isPresent()) {
            form.setError(error.get());
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
        showChallengeScreen(Optional.empty(), false);
    }

    private void showChallengeScreen(Optional<String> error) {
        showChallengeScreen(error, false);
    }

    private void showChallengeScreen(Optional<String> error, boolean smsResent) {
        final LoginFormsProvider form = context.form();
        if (error.isPresent()) {
            form.setError(error.get());
            form.setAttribute(SMS_AUTH_CODE_INVALID, true);
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
            return Optional.of("sms.phoneNumber.error.empty");
        }

        final SmsCodeConfiguration smsCodeConfiguration = new SmsCodeConfiguration(context.getConfig().getConfig());
        final String regex = smsCodeConfiguration.getPhoneNumberValidationRegex();
        if (regex != null && !regex.isEmpty() && !phoneNumber.matches(regex)) {
            return Optional.of("sms.phoneNumber.error.invalidFormat");
        }

        return Optional.empty();
    }

    private Optional<String> validateCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.of("sms.code.error.empty");
        }
        if (!new SmsChallenge(context.getAuthenticationSession()).isValid(code)) {
            return Optional.of("sms.code.error.wrong");
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

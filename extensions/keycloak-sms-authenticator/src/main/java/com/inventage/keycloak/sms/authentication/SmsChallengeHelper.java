package com.inventage.keycloak.sms.authentication;

import com.inventage.keycloak.sms.Constants;
import com.inventage.keycloak.sms.gateway.SmsServiceProvider;
import com.inventage.keycloak.sms.models.credential.SmsChallenge;
import com.inventage.keycloak.sms.models.credential.SmsCredentialModel;
import com.inventage.keycloak.sms.theme.SmsTextService;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.io.IOException;
import java.util.Optional;

/**
 * Shared helper for SMS challenge operations used by authenticators and required actions.
 */
public final class SmsChallengeHelper {

    private static final Logger LOGGER = Logger.getLogger(SmsChallengeHelper.class);

    private SmsChallengeHelper() {
    }

    /**
     * Sends an SMS challenge code to the given mobile number.
     *
     * @throws IllegalStateException if the configured SmsServiceProvider is not found
     */
    public static void sendSmsChallenge(
            String mobileNumber,
            SmsCodeConfiguration smsCodeConfiguration,
            AuthenticationSessionModel authSession,
            KeycloakSession session,
            UserModel user,
            SmsTextService smsTextService) throws IOException {

        final SmsServiceProvider smsServiceProvider = session.getProvider(SmsServiceProvider.class, smsCodeConfiguration.getSmsServiceProviderId());
        if (smsServiceProvider == null) {
            final IllegalStateException exception = new IllegalStateException("Sms Service Provider is null");
            LOGGER.warnf(exception, "sendSmsChallenge: SMS couldn't be sent, because SmsServiceProvider '%s' not found!", smsCodeConfiguration.getSmsServiceProviderId());
            throw exception;
        }

        final String code = new SmsChallenge(authSession).code(smsCodeConfiguration);
        final String smsText = smsTextService.getSmsText(code, smsCodeConfiguration.getSmsCodeTtl(), session.getContext().resolveLocale(user));
        smsServiceProvider.getSmsService().send(mobileNumber, smsText);
    }

    /**
     * Validates the entered SMS code against the challenge stored in the session.
     *
     * @return error message key, or empty if valid
     */
    public static Optional<String> validateCode(String code, AuthenticationSessionModel authSession) {
        if (code == null || code.isBlank()) {
            return Optional.of("sms.code.error.empty");
        }
        if (!new SmsChallenge(authSession).isValid(code)) {
            return Optional.of("sms.code.error.wrong");
        }
        return Optional.empty();
    }

    /**
     * Reads the phone number from the user's stored SMS credential.
     *
     * @return the phone number, or empty if no SMS credential exists
     */
    public static Optional<String> getMobileNumber(UserModel user) {
        return user.credentialManager()
                .getStoredCredentialsByTypeStream(SmsCredentialModel.TYPE)
                .findFirst()
                .map(credentialModel -> SmsCredentialModel.createFromCredentialModel(credentialModel).getSmsCredentialData().getPhoneNumber());
    }

    /**
     * Checks whether the form submission is a resend-SMS request.
     */
    public static boolean isResendSms(MultivaluedMap<String, String> formParams) {
        return formParams.getFirst(Constants.RESEND_SMS) != null;
    }
}

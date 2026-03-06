package com.inventage.keycloak.sms.authentication;

import com.inventage.keycloak.sms.Constants;
import com.inventage.keycloak.sms.gateway.SmsServiceProvider;
import com.inventage.keycloak.sms.models.credential.SmsChallenge;
import com.inventage.keycloak.sms.models.credential.SmsCredentialModel;
import com.inventage.keycloak.sms.theme.SmsTextService;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.io.IOException;
import java.util.Optional;

import static com.inventage.keycloak.sms.authentication.SmsCodeValidationResult.EMPTY;
import static com.inventage.keycloak.sms.authentication.SmsCodeValidationResult.EXPIRED;
import static com.inventage.keycloak.sms.authentication.SmsCodeValidationResult.VALID;
import static com.inventage.keycloak.sms.authentication.SmsCodeValidationResult.WRONG;

/**
 * Shared helper for SMS challenge operations used by authenticators and required actions.
 */
public final class SmsChallengeHelper {

    private static final Logger LOGGER = Logger.getLogger(SmsChallengeHelper.class);

    /** Message key shown when the user is temporarily locked out by brute force protection. */
    public static final String SMS_AUTH_CODE_TEMPORARILY_DISABLED = "smsAuthCodeTemporarilyDisabled";
    /** Message key shown when the user is permanently locked out by brute force protection. */
    public static final String SMS_AUTH_CODE_PERMANENTLY_DISABLED = "smsAuthCodePermanentlyDisabled";

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
     * Checks if the user is disabled by brute force protection.
     * Convenience overload for required actions, mirroring
     * {@link AuthenticatorUtils#getDisabledByBruteForceEventError(org.keycloak.authentication.AuthenticationFlowContext, UserModel)}.
     *
     * @return the brute force event error string, or {@code null} if the user is not locked out
     */
    public static String getDisabledByBruteForceEventError(RequiredActionContext context) {
        final KeycloakSession session = context.getSession();
        return AuthenticatorUtils.getDisabledByBruteForceEventError(
                session.getProvider(BruteForceProtector.class), session, context.getRealm(), context.getUser());
    }

    /**
     * Maps a brute force event error to the corresponding i18n message key.
     */
    public static String disabledByBruteForceError(String bruteForceError) {
        if (Errors.USER_TEMPORARILY_DISABLED.equals(bruteForceError)) {
            return SMS_AUTH_CODE_TEMPORARILY_DISABLED;
        }
        return SMS_AUTH_CODE_PERMANENTLY_DISABLED;
    }

    /**
     * Registers a failed login attempt with the brute force protector.
     * Only needed for required actions since the authenticator framework handles this automatically.
     * <p>
     * <b>Important:</b> When calling both {@link #getDisabledByBruteForceEventError} and this method in the same request,
     * {@code getDisabledByBruteForceEventError} must be called <b>before</b> this method. Reversing the order causes a race
     * condition with Keycloak's {@code DefaultBlockingBruteForceProtector} internal state.
     */
    public static void registerFailedAttempt(RequiredActionContext context) {
        if (!context.getRealm().isBruteForceProtected()) {
            return;
        }
        final BruteForceProtector protector = context.getSession().getProvider(BruteForceProtector.class);
        if (protector != null) {
            protector.failedLogin(context.getRealm(), context.getUser(), context.getConnection(), context.getUriInfo());
        }
    }

    /**
     * Validates the entered SMS code against the challenge stored in the session, including expiry.
     */
    public static SmsCodeValidationResult validateCode(String code, AuthenticationSessionModel authSession) {
        if (code == null || code.isBlank()) {
            return EMPTY;
        }
        final SmsChallenge smsChallenge = new SmsChallenge(authSession);
        if (!smsChallenge.isValid(code)) {
            return WRONG;
        }
        if (smsChallenge.isExpired()) {
            return EXPIRED;
        }
        return VALID;
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

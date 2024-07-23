package com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.authenticator;

import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.AuthenticationSMSUtil;
import com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.credential.SmsCredentialModel;
import com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.requireaction.SmsRequiredAction;

/**
 * @author Geraldine von Roten
 */
public class SmsAuthenticator implements Authenticator {

    private static final String TPL_CODE = "login-sms.ftl";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        try {
            AuthenticationSMSUtil.from(context).sendSms();
            context.challenge(context.form().setAttribute("realm", context.getRealm()).createForm(TPL_CODE));
        } catch (Exception e) {
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().setError("smsAuthSmsNotSent", e.getMessage())
                            .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        String enteredCode = context.getHttpRequest().getDecodedFormParameters().getFirst("code");
        AuthenticationSMSUtil smsUtil = AuthenticationSMSUtil.from(context);

        if (smsUtil.isValid(enteredCode)) {
            if (smsUtil.isExpired()) {
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
            if (execution.isRequired()) {
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                        context.form().setAttribute("realm", context.getRealm())
                                .setError("smsAuthCodeInvalid").createForm(TPL_CODE));
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
package com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.forms;

import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.authentication.forms.RegistrationPage;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.validation.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidateEmailFormAction implements FormAction, FormActionFactory
{
    public static final String PROVIDER_ID = "registration-validateemail-action";

    private static final String FIELD_EMAIL_CONFIRM = "emailconfirm";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public String getDisplayType() {
        return "validateemail";
    }

    @Override
    public String getReferenceCategory() {
        return "email";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        form.setAttribute("emailConfirmRequired", true);
    }

    @Override
    public void validate(ValidationContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        List<FormMessage> errors = new ArrayList<>();
        context.getEvent().detail(Details.REGISTER_METHOD, "form");
        if (!Validation.isBlank(formData.getFirst(RegistrationPage.FIELD_EMAIL))) {
            if (!formData.getFirst(RegistrationPage.FIELD_EMAIL).equals(formData.getFirst(FIELD_EMAIL_CONFIRM))) {
                errors.add(new FormMessage(FIELD_EMAIL_CONFIRM, "invalidEmailConfirmMessage"));
            }
        }

        if (errors.size() > 0) {
            context.error(Errors.INVALID_REGISTRATION);
            context.validationError(formData, errors);
        } else {
            context.success();
        }
    }

    @Override
    public void success(FormContext context) {
        context.getUser().setEmailVerified(true);
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        //NOP
    }

    @Override
    public String getHelpText() {
        return "Checks both entered e-mails";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public FormAction create(KeycloakSession keycloakSession) {
        return this;
    }

    @Override
    public void init(Config.Scope scope) {
        //NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        //NOP
    }

    @Override
    public void close() {
        //NOP
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}

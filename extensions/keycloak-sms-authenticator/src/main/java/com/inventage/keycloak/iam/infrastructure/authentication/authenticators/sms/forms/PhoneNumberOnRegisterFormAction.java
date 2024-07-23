package com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.forms;

import com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.requireaction.SmsRequiredAction;
import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.validation.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PhoneNumberOnRegisterFormAction implements FormAction, FormActionFactory
{
    public static final String PROVIDER_ID = "registration-phonenumber-action";

    private static final String FIELD_PHONENUMBER = "phonenumber";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public String getDisplayType() {
        return "phonenumber";
    }

    @Override
    public String getReferenceCategory() {
        return "phonenumber";
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
        form.setAttribute("phonenumberRequired", true);
    }

    @Override
    public void validate(ValidationContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        List<FormMessage> errors = new ArrayList<>();

        if (Validation.isBlank(formData.getFirst(FIELD_PHONENUMBER))) {
            errors.add(new FormMessage(FIELD_PHONENUMBER, "sms.phoneNumber.error.empty"));
        }

        //TODO add more validations for phonenumber?

        if (errors.isEmpty()){
            context.success();
        }
        else {
            context.error(Errors.INVALID_REGISTRATION);
            context.validationError(formData, errors);
        }
    }

    @Override
    public void success(FormContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String enteredNumber = formData.getFirst(FIELD_PHONENUMBER);

        context.getAuthenticationSession().setAuthNote(SmsRequiredAction.ENTERED_NUMBER_KEY, enteredNumber);
        context.getAuthenticationSession().addRequiredAction(SmsRequiredAction.PROVIDER_ID);
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
        return "Asks the user for their phone number on the registration page.";
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

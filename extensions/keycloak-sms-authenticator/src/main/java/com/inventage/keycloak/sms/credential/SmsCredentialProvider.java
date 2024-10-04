package com.inventage.keycloak.sms.credential;

import com.inventage.keycloak.sms.authentication.requireactions.SmsRequiredAction;
import com.inventage.keycloak.sms.models.credential.SmsCredentialModel;
import com.inventage.keycloak.sms.models.credential.dto.SmsCredentialData;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class SmsCredentialProvider implements CredentialProvider<SmsCredentialModel> {


    private static final Logger logger = Logger.getLogger(SmsCredentialProvider.class);

    private final KeycloakSession session;

    public SmsCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, SmsCredentialModel credentialModel) {
        if (credentialModel.getCreatedDate() == null) {
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        }

        return allowMultipleCredentials() ? addCredential(realm, user, credentialModel) : updateCredential(realm, user, credentialModel);
    }

    private boolean allowMultipleCredentials() {
        return true;
    }

    private CredentialModel addCredential(RealmModel realm, UserModel user, SmsCredentialModel credentialModel) {
        if (samePhoneNumberAlreadyConfigured(user, credentialModel)) {
            return updateCredential(realm, user, credentialModel);
        }

        return user.credentialManager().createStoredCredential(credentialModel);
    }

    private boolean samePhoneNumberAlreadyConfigured(UserModel user, SmsCredentialModel credentialModel) {
        Optional<CredentialModel> smsCredentialModel = getCredentialModelWithSamePhoneNumber(user, credentialModel);

        return smsCredentialModel.isPresent();
    }

    private Optional<CredentialModel> getCredentialModelWithSamePhoneNumber(UserModel user, SmsCredentialModel credentialModel) {
        return user.credentialManager().getStoredCredentialsByTypeStream(getType())
            .filter
                (
                    cred -> {
                        final SmsCredentialData smsCredentialData;
                        try {
                            smsCredentialData = JsonSerialization.readValue(cred.getCredentialData(), SmsCredentialData.class);
                            return Objects.equals(smsCredentialData.getPhoneNumber(), credentialModel.getSmsCredentialData().getPhoneNumber());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                )
            .findFirst();
    }

    // replace the sms credential if there is already one
    private CredentialModel updateCredential(RealmModel realm, UserModel user, SmsCredentialModel credentialModel) {
        CredentialModel smsCredentialModel = getSmsCredentialModel(realm, user);
        if (smsCredentialModel != null) {
            credentialModel.setId(smsCredentialModel.getId());
            user.credentialManager().updateStoredCredential(credentialModel);
            return credentialModel;
        } else {
            return addCredential(realm, user, credentialModel);
        }
    }

    private CredentialModel getSmsCredentialModel(RealmModel realm, UserModel user) {
        return user.credentialManager().getStoredCredentialsByTypeStream(getType())
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        logger.debugv("Delete Sms credential. username = {0}, credentialId = {1}", user.getUsername(), credentialId);
        return user.credentialManager().removeStoredCredentialById(credentialId);
    }

    @Override
    public SmsCredentialModel getCredentialFromModel(CredentialModel model) {
        return SmsCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public String getType() {
        return SmsCredentialModel.TYPE;
    }


    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        return CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.TWO_FACTOR)
                .displayName("sms.displayName")
                .helpText("sms.helpText")
                .iconCssClass("kcAuthenticatorSmsClass")
                .createAction(SmsRequiredAction.PROVIDER_ID)
                .removeable(true)
                .build(session);
    }
}

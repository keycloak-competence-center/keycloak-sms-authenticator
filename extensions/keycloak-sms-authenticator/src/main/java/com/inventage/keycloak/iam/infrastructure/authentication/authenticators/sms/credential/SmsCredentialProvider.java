package com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.credential;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.requireaction.SmsRequiredAction;

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

        CredentialModel smsCredentialModel = getSmsCredentialModel(realm, user);
        if (smsCredentialModel != null) {
            credentialModel.setId(smsCredentialModel.getId());
            user.credentialManager().updateStoredCredential(credentialModel);
            return credentialModel;
        }
        else {
            return user.credentialManager().createStoredCredential(credentialModel);
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
                .updateAction(SmsRequiredAction.PROVIDER_ID)
                .removeable(true)
                .build(session);
    }
}

package com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.credential;

import java.io.IOException;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.util.JsonSerialization;

import com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.credential.dto.SmsCredentialData;
import com.inventage.keycloak.iam.infrastructure.authentication.authenticators.sms.credential.dto.SmsSecretData;

public class SmsCredentialModel extends CredentialModel {

        public static final String TYPE = "sms";

        private final SmsCredentialData credentialData;
        private final SmsSecretData secretData;

        private SmsCredentialModel(SmsCredentialData credentialData, SmsSecretData secretData) {
            this.credentialData = credentialData;
            this.secretData = secretData;
            setType(TYPE);
        }

        public static SmsCredentialModel create(String phoneNumber) {
            SmsCredentialData credentialData = new SmsCredentialData(phoneNumber);
            SmsSecretData secretData = new SmsSecretData();

            SmsCredentialModel credentialModel = new SmsCredentialModel(credentialData, secretData);
            credentialModel.fillCredentialModelFields();
            credentialModel.setUserLabel(createUserLabel(phoneNumber));
            return credentialModel;
        }

        private static String createUserLabel(String phoneNumber) {
            return phoneNumber;
        }


        public static SmsCredentialModel createFromCredentialModel(CredentialModel credentialModel) {
            try {
                SmsCredentialData credentialData = JsonSerialization.readValue(credentialModel.getCredentialData(), SmsCredentialData.class);
                SmsSecretData secretData = JsonSerialization.readValue(credentialModel.getSecretData(), SmsSecretData.class);

                SmsCredentialModel smsCredentialModel = new SmsCredentialModel(credentialData, secretData);
                smsCredentialModel.setUserLabel(credentialModel.getUserLabel());
                smsCredentialModel.setCreatedDate(credentialModel.getCreatedDate());
                smsCredentialModel.setType(TYPE);
                smsCredentialModel.setId(credentialModel.getId());
                smsCredentialModel.setSecretData(credentialModel.getSecretData());
                smsCredentialModel.setCredentialData(credentialModel.getCredentialData());
                return smsCredentialModel;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public SmsCredentialData getSmsCredentialData() {
            return credentialData;
        }

        private void fillCredentialModelFields() {
            try {
                setCredentialData(JsonSerialization.writeValueAsString(credentialData));
                setSecretData(JsonSerialization.writeValueAsString(secretData));
                setCreatedDate(Time.currentTimeMillis());
                setType(TYPE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        @Override
        public String toString() {
            return "SmsCredentialModel { " +
                    getType() +
                    ", " + getUserLabel() +
                    ", " + credentialData +
                    ", " + secretData +
                    " }";
        }

}

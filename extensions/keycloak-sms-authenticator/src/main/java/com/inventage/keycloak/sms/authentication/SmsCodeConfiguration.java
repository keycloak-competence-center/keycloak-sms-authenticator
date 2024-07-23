package com.inventage.keycloak.sms.authentication;

import com.inventage.keycloak.sms.infrastructure.gateway.console.SmsToConsoleProviderFactory;
import com.inventage.keycloak.sms.models.credential.SmSChallengeConfiguration;
import org.jboss.logging.Logger;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Map;

// Shared in SmsAuthenticator and SmsRequiredAction
public class SmsCodeConfiguration implements SmSChallengeConfiguration {

    public static final String SMS_SERVICE_PROVIDER_ID_CONFIG = "sms-service-provider-id";
    public static final String SMS_SERVICE_PROVIDER_ID_DEFAULT = SmsToConsoleProviderFactory.PROVIDER_ID;

    public static final String SMS_CODE_LENGTH_CONFIG = "sms-code-length";
    public static final int SMS_CODE_LENGTH_DEFAULT = 6;

    public static final String SMS_CODE_TTL_CONFIG = "sms-code-ttl";
    public static final int SMS_CODE_TTL_DEFAULT = 300; // in seconds

    public static final String SMS_CODE_CHARACTERS_CONFIG = "sms-code-characters";
    public static final String SMS_CODE_CHARACTERS_DEFAULT = null;

    public static List<ProviderConfigProperty> getConfigProperties() {
        final ProviderConfigProperty smsServiceProviderId = new ProviderConfigProperty();
        smsServiceProviderId.setType(ProviderConfigProperty.STRING_TYPE);
        smsServiceProviderId.setDefaultValue(SMS_SERVICE_PROVIDER_ID_DEFAULT);
        smsServiceProviderId.setName(SMS_SERVICE_PROVIDER_ID_CONFIG);
        smsServiceProviderId.setLabel("Sms Service Provider Id");
        smsServiceProviderId.setHelpText("SMS Service Provider to be used for sending out SMS, see Provider Info 'sms-service' for possible values.");

        final ProviderConfigProperty smsCodeLength = new ProviderConfigProperty();
        smsCodeLength.setType(ProviderConfigProperty.STRING_TYPE);
        smsCodeLength.setDefaultValue(SMS_CODE_LENGTH_DEFAULT);
        smsCodeLength.setName(SMS_CODE_LENGTH_CONFIG);
        smsCodeLength.setLabel("SMS code Length");
        smsCodeLength.setHelpText("Length of SMS code to be used for sending out SMS.");

        final ProviderConfigProperty smsCodeTtl = new ProviderConfigProperty();
        smsCodeTtl.setType(ProviderConfigProperty.STRING_TYPE);
        smsCodeTtl.setDefaultValue(SMS_CODE_TTL_DEFAULT);
        smsCodeTtl.setName(SMS_CODE_TTL_CONFIG);
        smsCodeTtl.setLabel("SMS code time to live in seconds");
        smsCodeTtl.setHelpText("Time in seconds the SMS code is valid.");

        final ProviderConfigProperty smsCodeCharacters = new ProviderConfigProperty();
        smsCodeCharacters.setType(ProviderConfigProperty.STRING_TYPE);
        smsCodeCharacters.setDefaultValue(SMS_CODE_CHARACTERS_DEFAULT);
        smsCodeCharacters.setName(SMS_CODE_CHARACTERS_CONFIG);
        smsCodeCharacters.setLabel("Characters to be used for the SMS code");
        smsCodeCharacters.setHelpText("If not specified, the value is taken from Keycloak SecretGenerator");

        return List.of(smsServiceProviderId, smsCodeLength, smsCodeTtl, smsCodeCharacters);
    }

    private static final Logger LOGGER = Logger.getLogger(SmsCodeConfiguration.class);

    private final Map<String, String> config;

    public SmsCodeConfiguration(Map<String, String> config) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        this.config = config;
    }

    public String getSmsServiceProviderId() {
        return getStringFromConfig(SMS_SERVICE_PROVIDER_ID_CONFIG, SMS_SERVICE_PROVIDER_ID_DEFAULT);
    }

    @Override
    public int getSmsCodeLength() {
        return getIntFromConfig(SMS_CODE_LENGTH_CONFIG, SMS_CODE_LENGTH_DEFAULT);
    }

    @Override
    public int getSmsCodeTtl() {
        return getIntFromConfig(SMS_CODE_TTL_CONFIG, SMS_CODE_TTL_DEFAULT);
    }

    @Override
    public String getSmsCodeCharacters() {
        return getStringFromConfig(SMS_CODE_CHARACTERS_CONFIG, SMS_CODE_CHARACTERS_DEFAULT);
    }

    protected String getStringFromConfig(String configKey, String defaultValue) {
        final String configValue = config.get(configKey);
        if (configValue != null) {
            return configValue;
        }
        return defaultValue;
    }

    protected int getIntFromConfig(String configKey, int defaultValue) {
        final String configValue = config.get(configKey);
        if (configValue != null) {
            int value = defaultValue;
            try {
                value = Integer.parseInt(configValue);
            }
            catch (NumberFormatException e) {
                LOGGER.warnf("getIntFromConfig: config value '%s' is not an integer, returning default value '%s'.", configValue, value);
            }
            return value;
        }
        return defaultValue;
    }

}

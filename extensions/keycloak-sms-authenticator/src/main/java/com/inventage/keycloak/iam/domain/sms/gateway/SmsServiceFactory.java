package com.inventage.keycloak.iam.domain.sms.gateway;

import com.inventage.keycloak.iam.domain.sms.config.SmsConfig;
import com.inventage.keycloak.iam.infrastructure.portal.UniportSmsFacade;
import org.apache.http.client.HttpClient;
import org.jboss.logging.Logger;

import com.inventage.keycloak.iam.infrastructure.portal.UniportSmsAdapter;

/**
 * @author Geraldine von Roten
 */
public class SmsServiceFactory {

    private static final Logger LOG = Logger.getLogger(SmsServiceFactory.class);

    public static SmsService get(SmsConfig config, HttpClient httpClient) {
        if (config.isSimulation()) {
            return (phoneNumber, message) ->
                    LOG.warn(String.format("***** SIMULATION MODE ***** Would send SMS to %s with text: %s", phoneNumber, message));
        } else {
            return new UniportSmsAdapter(config, new UniportSmsFacade(httpClient, config.getServiceUrl()));
        }
    }

}
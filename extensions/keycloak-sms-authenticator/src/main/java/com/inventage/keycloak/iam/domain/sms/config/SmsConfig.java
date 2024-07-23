package com.inventage.keycloak.iam.domain.sms.config;

import org.keycloak.models.RealmModel;

public class SmsConfig {

    private int length;
    private int ttl; //time to live in seconds
    private String allowedCharactersInCode;
    private String senderAddress;
    private boolean simulation;
    private String serviceUrl;

    private SmsConfig(RealmModel realm) {
        length = realm.getAttribute("sms.length", 6);
        ttl = realm.getAttribute("sms.ttl", 300);
        allowedCharactersInCode = realm.getAttribute("sms.allowedCharactersInCode");
        simulation = realm.getAttribute("sms.simulation", true);
        senderAddress = realm.getAttribute("sms.senderAddress") != null ? realm.getAttribute("sms.senderAddress") : "Keycloak" ;
        serviceUrl = realm.getAttribute("sms.serviceUrl") != null ? realm.getAttribute("sms.serviceUrl") : "http://localhost:20102/sms/send";
    }

    public static SmsConfig fromRealm(RealmModel realm) {
        return new SmsConfig(realm);
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public boolean isSimulation() {
        return simulation;
    }

    public void setSimulation(boolean simulation) {
        this.simulation = simulation;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getAllowedCharactersInCode() {
        return allowedCharactersInCode;
    }

    public void setAllowedCharactersInCode(String allowedCharactersInCode) {
        this.allowedCharactersInCode = allowedCharactersInCode;
    }
}

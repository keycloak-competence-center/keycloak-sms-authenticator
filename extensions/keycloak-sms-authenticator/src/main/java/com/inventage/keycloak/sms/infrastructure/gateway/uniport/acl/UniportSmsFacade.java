package com.inventage.keycloak.sms.infrastructure.gateway.uniport.acl;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class UniportSmsFacade {

    private final UniportSmsTranslator translator;
    private final HttpClient httpClient;
    private final String serviceUrl;

    public UniportSmsFacade(HttpClient httpClient, String serviceUrl) {
        this.translator = new UniportSmsTranslator();
        this.httpClient = httpClient;
        this.serviceUrl = serviceUrl;
    }

    protected HttpResponse post(SmsSendRequest sendRequest) {
        try {
            final HttpPost httpPost = new HttpPost(serviceUrl);
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            httpPost.setEntity(new StringEntity(translator.toJson(sendRequest), StandardCharsets.UTF_8));
            return httpClient.execute(httpPost);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

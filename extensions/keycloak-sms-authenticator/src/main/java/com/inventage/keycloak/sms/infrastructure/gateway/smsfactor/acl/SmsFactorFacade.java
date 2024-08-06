package com.inventage.keycloak.sms.infrastructure.gateway.smsfactor.acl;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Facade for the <a href="https://www.smsfactor.com/">SMS Factor</a> API according to the DDD anti-corruption-layer pattern.
 */
public class SmsFactorFacade {

    private final HttpClient httpClient;
    private final String serviceUrl;
    private final String token;

    public SmsFactorFacade(HttpClient httpClient, String serviceUrl, String token) {
        this.httpClient = httpClient;
        this.serviceUrl = serviceUrl;
        this.token = token;
    }

    /**
     * Executes an HTTP GET requests depending on the {@link SmsFactorSendRequest}.
     * <p>
     * The GET request consists of following query parameters:
     *
     * <ul>
     *     <li>text</li>
     *     <li>to</li>
     *     <li>sender</li>
     * </ul>
     *
     * and of following headers:
     *
     * <ul>
     *     <li>Accept</li>
     *     <li>Authorization</li>
     * </ul>
     *
     * @return HTTP response of the GET request
     *
     * @apiNote <a href="https://dev.smsfactor.com/en/api/sms/send/send-single">send single message documentation</a>
     */
    /* package private */ HttpResponse get(SmsFactorSendRequest sendRequest) {
        try {
            final HttpGet httpGet = createHttpGetRequest(sendRequest);
            return httpClient.execute(httpGet);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpGet createHttpGetRequest(SmsFactorSendRequest sendRequest) throws URISyntaxException {
        final HttpGet httpGet = new HttpGet(serviceUrl);
        final URI uriWithQueryParameters = new URIBuilder(httpGet.getURI())
                .addParameter("text", sendRequest.messageContent())
                .addParameter("to", sendRequest.recipient())
                .addParameter("sender", sendRequest.sender())
                .build();

        httpGet.setURI(uriWithQueryParameters);
        final Header acceptHeader = new BasicHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        httpGet.addHeader(acceptHeader);
        final Header authorizationHeader = new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        httpGet.addHeader(authorizationHeader);
        return httpGet;
    }
}

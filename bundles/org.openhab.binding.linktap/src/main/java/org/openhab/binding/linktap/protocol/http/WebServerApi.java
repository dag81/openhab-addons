/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.linktap.protocol.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.*;
import static org.openhab.binding.linktap.protocol.http.NotTapLinkGatewayException.*;
import static org.openhab.binding.linktap.protocol.http.TransientCommunicationIssueException.*;

import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.HttpMethod;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpFields;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link WebServerApi} defines interactions with the web server interface.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public final class WebServerApi {

    private final Logger logger = LoggerFactory.getLogger(WebServerApi.class);

    private @NonNullByDefault({}) HttpClient httpClient;

    final int REQ_TIMEOUT_SECONDS = 3;

    private static final WebServerApi INSTANCE = new WebServerApi();

    private WebServerApi() {
    }

    public static WebServerApi getInstance() {
        return INSTANCE;
    }

    /**
     * Sets the httpClient object to be used for API calls to Vesync.
     *
     * @param httpClient the client to be used.
     */
    public void setHttpClient(@Nullable HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Map<String, String> getBridgeProperities(final String hostname)
            throws NotTapLinkGatewayException, TransientCommunicationIssueException {
        try {
            final Request request = httpClient.newRequest(URI_HOST_PREFIX + hostname).method(HttpMethod.GET);
            final ContentResponse cr = request.timeout(REQ_TIMEOUT_SECONDS, TimeUnit.SECONDS).send();
            if (HttpURLConnection.HTTP_OK != cr.getStatus()) {
                throw new NotTapLinkGatewayException(UNEXPECTED_STATUS_CODE);
            }
            ValidateHeaders(cr.getHeaders());
            final String responseData = cr.getContentAsString();
            final Document doc = Jsoup.parse(responseData);

            switch (doc.title()) {
                case TITLE_API_CONFIG_PAGE:
                    break;
                case TITLE_API_LOGIN_PAGE:
                    return Map.of();
                default:
                    throw new NotTapLinkGatewayException(MISSING_SERVER_TITLE);
            }

            return getMetadataProperties(doc);

        } catch (InterruptedException | TimeoutException e) {
            throw new TransientCommunicationIssueException(HOST_COMM_TIMEOUT);
        } catch (ExecutionException e) {
            final Throwable t = e.getCause();
            if (t instanceof UnknownHostException) {
                throw new TransientCommunicationIssueException(HOST_NOT_RESOLVED);
            } else if (t instanceof SocketTimeoutException) {
                throw new TransientCommunicationIssueException(HOST_UNREACHABLE);
            } else if (t instanceof SSLHandshakeException) {
                throw new NotTapLinkGatewayException(UNEXPECTED_HTTPS);
            } else {
                logger.warn("ExecutionException -> {}", e.getMessage());
            }
            throw new NotTapLinkGatewayException("Unexpected failure -> " + e.getMessage());
        }
    }

    /**
     * Extract the common properties for all devices, from the given meta-data of a device.
     *
     * @param doc - the html document returns from the potential Gateway device
     * @return - Map of common props
     */
    private Map<String, String> getMetadataProperties(final Document doc) {

        final Map<String, String> newProps = new HashMap<>(4);

        /*
         * Extract elements based on td location using the text markers
         */
        String firmwareVer = "?";
        String hwModel = "?";
        String id = "?";
        String macAddr = "?";

        final org.jsoup.select.Elements tdEntries = doc.getElementsByTag("td");
        for (int i = 0; i < tdEntries.size(); ++i) {
            if (tdEntries.get(i).hasText()) {
                switch (tdEntries.get(i).text()) {
                    case "Firmware version":
                        firmwareVer = tdEntries.get(i + 1).text();
                        i++;
                        break;
                    case "Model":
                        hwModel = tdEntries.get(i + 1).text();
                        i++;
                        break;
                    case "ID":
                        id = tdEntries.get(i + 1).text();
                        i++;
                        break;
                    case "MAC address":
                        macAddr = tdEntries.get(i + 1).text();
                        i++;
                        break;

                }
            }
        }

        newProps.put(BRIDGE_PROP_GW_ID, id);
        newProps.put(BRIDGE_PROP_GW_VER, firmwareVer);
        newProps.put(BRIDGE_PROP_MAC_ADDR, macAddr);
        newProps.put(BRIDGE_PROP_HW_MODEL, hwModel);

        /*
         * Extract elements based on name markers and attributes
         */
        final boolean httpApiEnabled = doc.getElementsByAttributeValue("name", "htapi").hasAttr("checked");
        final String httpApiEndpoint = doc.getElementsByAttributeValue("name", "URL").attr("value");

        newProps.put(BRIDGE_HTTP_API_ENABLED, String.valueOf(httpApiEnabled));
        newProps.put(BRIDGE_HTTP_API_EP, httpApiEndpoint);

        return newProps;
    }

    public boolean unlockWebInterface(final String hostname, final String username, final String password)
            throws NotTapLinkGatewayException, TransientCommunicationIssueException {
        try {
            org.eclipse.jetty.util.Fields fields = new org.eclipse.jetty.util.Fields();
            fields.put(FIELD_ADMIN_USER, username);
            fields.put(FIELD_ADMIN_USER_PWD, password);
            final Request request = httpClient.newRequest(URI_HOST_PREFIX + hostname + "/login.shtml")
                    .method(HttpMethod.POST).content(new FormContentProvider(fields));
            final ContentResponse cr = request.timeout(REQ_TIMEOUT_SECONDS, TimeUnit.SECONDS).send();
            if (HttpURLConnection.HTTP_OK != cr.getStatus()) {
                throw new NotTapLinkGatewayException(UNEXPECTED_STATUS_CODE);
            }
            ValidateHeaders(cr.getHeaders());
            return !getBridgeProperities(hostname).isEmpty();
        } catch (InterruptedException | TimeoutException e) {
            throw new TransientCommunicationIssueException(HOST_COMM_TIMEOUT);
        } catch (ExecutionException e) {
            final Throwable t = e.getCause();
            if (t instanceof UnknownHostException) {
                throw new TransientCommunicationIssueException(HOST_NOT_RESOLVED);
            } else if (t instanceof SocketTimeoutException) {
                throw new TransientCommunicationIssueException(HOST_UNREACHABLE);
            } else if (t instanceof SSLHandshakeException) {
                throw new NotTapLinkGatewayException(UNEXPECTED_HTTPS);
            } else {
                logger.warn("ExecutionException -> {}", e.getMessage());
            }
            throw new NotTapLinkGatewayException("Unexpected failure -> " + e.getMessage());
        }
    }

    /**
     * Returns whether a response from the HTTP endpoint reached, appears to have the correct
     * header markers for a Link Tap Gateway device.
     *
     * @param headers the http headers from the response to be checked
     * @throws NotTapLinkGatewayException if the response does not appear to be from a Link Tap Gateway
     */
    private void ValidateHeaders(final HttpFields headers) throws NotTapLinkGatewayException {
        if (!headers.contains(HEADER_SERVER, HEADER_GW_SERVER_NAME)) {
            throw new NotTapLinkGatewayException(HEADERS_MISSING);
        }
    }

    public String sendRequest(final String hostname, final String requestBody)
            throws NotTapLinkGatewayException, TransientCommunicationIssueException {
        try {
            final Request request = httpClient.POST(URI_HOST_PREFIX + hostname + "/api.shtml");
            request.content(new StringContentProvider(requestBody), APPLICATION_JSON_TYPE.toString());

            final ContentResponse cr = request.timeout(REQ_TIMEOUT_SECONDS, TimeUnit.SECONDS).send();
            if (HttpURLConnection.HTTP_OK != cr.getStatus()) {
                throw new NotTapLinkGatewayException(UNEXPECTED_STATUS_CODE);
            }
            ValidateHeaders(cr.getHeaders());
            String responseData = cr.getContentAsString();
            final Document doc = Jsoup.parse(responseData);
            final String docTitle = doc.title();
            if (!docTitle.equals(TITLE_API_RESPONSE))
                throw new NotTapLinkGatewayException(MISSING_API_TITLE);
            responseData = doc.body().text();
            return responseData;
        } catch (InterruptedException | TimeoutException e) {
            throw new TransientCommunicationIssueException(HOST_COMM_TIMEOUT);
        } catch (ExecutionException e) {
            final Throwable t = e.getCause();
            if (t instanceof UnknownHostException) {
                throw new TransientCommunicationIssueException(HOST_NOT_RESOLVED);
            } else if (t instanceof SocketTimeoutException) {
                throw new TransientCommunicationIssueException(HOST_UNREACHABLE);
            } else if (t instanceof SSLHandshakeException) {
                throw new NotTapLinkGatewayException(UNEXPECTED_HTTPS);
            } else {
                logger.warn("ExecutionException -> {}", e.getMessage());
            }
            throw new NotTapLinkGatewayException("Unexpected failure -> " + e.getMessage());
        }
    }

    public final static String URI_SCHEME = "http";
    public final static String URI_HOST_PREFIX = URI_SCHEME + "://";

    /**
     * Headers
     */
    public final static String HEADER_SERVER = "Server";
    public final static String HEADER_GW_SERVER_NAME = "LinkTap Gateway";

    /**
     * HTML title field mappings to use cases
     */
    private final static String TITLE_API_RESPONSE = "api";
    private final static String TITLE_API_CONFIG_PAGE = "LinkTap Gateway";
    private final static String TITLE_API_LOGIN_PAGE = "LinkTap Gateway Login";

    /**
     * Field names for form submission API's
     */
    private final static String FIELD_ADMIN_USER = "admin";
    private final static String FIELD_ADMIN_USER_PWD = "adminpwd";
}

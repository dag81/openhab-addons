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

import java.net.*;
import java.util.Collections;
import java.util.Enumeration;
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

    private static WebServerApi INSTANCE = new WebServerApi();
    private static final Object InstanceLock = new Object();

    private WebServerApi() {
    }

    public static WebServerApi getInstance() {
        synchronized (InstanceLock) {
            if (INSTANCE == null) {
                INSTANCE = new WebServerApi();
            }
        }
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

    public boolean isWebServerUnlocked(final String hostname)
            throws NotTapLinkGatewayException, TransientCommunicationIssueException {
        try {
            final Request request = httpClient.newRequest("http://" + hostname).method(HttpMethod.GET);
            final ContentResponse cr = request.timeout(REQ_TIMEOUT_SECONDS, TimeUnit.SECONDS).send();
            if (HttpURLConnection.HTTP_OK != cr.getStatus()) {
                throw new NotTapLinkGatewayException("Unexpected status code received " + cr.getStatus());
            }
            ValidateHeaders(cr.getHeaders());
            String responseData = cr.getContentAsString();
            String docTitle = extractTitle(responseData);
            switch (docTitle) {
                case "LinkTap Gateway":
                    return true;
                case "LinkTap Gateway Login":
                    return false;
                default:
                    throw new NotTapLinkGatewayException("Unexpected title received: " + docTitle);
            }
        } catch (InterruptedException | TimeoutException e) {
            logger.warn("InterruptedException / TimeoutException -> {}", e.getMessage());
            throw new TransientCommunicationIssueException("Comm timeout or interrupted");
        } catch (ExecutionException e) {
            final Throwable t = e.getCause();
            if (t instanceof UnknownHostException) {
                throw new TransientCommunicationIssueException("Could not resolve host");
            } else if (t instanceof SocketTimeoutException) {
                throw new TransientCommunicationIssueException("Could not reach host");
            } else if (t instanceof SSLHandshakeException) {
                logger.warn("Device doesn't support SSL this is not the gateway device -> {}", e.getMessage());
            } else {
                logger.warn("ExecutionException -> {}", e.getMessage());
            }
            throw new NotTapLinkGatewayException("Unexpected failure -> " + e.getMessage());
        }
    }

    public boolean unlockWebInterface(final String hostname, final String username, final String password)
            throws NotTapLinkGatewayException, TransientCommunicationIssueException {
        try {
            org.eclipse.jetty.util.Fields fields = new org.eclipse.jetty.util.Fields();
            fields.put("admin", username);
            fields.put("adminpwd", password);
            final Request request = httpClient.newRequest("http://" + hostname + "/login.shtml").method(HttpMethod.POST)
                    .content(new FormContentProvider(fields));
            final ContentResponse cr = request.timeout(REQ_TIMEOUT_SECONDS, TimeUnit.SECONDS).send();
            if (HttpURLConnection.HTTP_OK != cr.getStatus()) {
                throw new NotTapLinkGatewayException("Unexpected status code received " + cr.getStatus());
            }
            ValidateHeaders(cr.getHeaders());
            findLocalAddressRoutableToDevice();
            return isWebServerUnlocked(hostname);
        } catch (InterruptedException | TimeoutException e) {
            logger.warn("InterruptedException / TimeoutException -> {}", e.getMessage());
            throw new TransientCommunicationIssueException("Comm timeout or interrupted");
        } catch (ExecutionException e) {
            final Throwable t = e.getCause();
            if (t instanceof UnknownHostException) {
                throw new TransientCommunicationIssueException("Could not resolve host");
            } else if (t instanceof SocketTimeoutException) {
                throw new TransientCommunicationIssueException("Could not reach host");
            } else if (t instanceof SSLHandshakeException) {
                logger.warn("Device doesn't support SSL this is not the gateway device -> {}", e.getMessage());
            } else {
                logger.warn("ExecutionException -> {}", e.getMessage());
            }
            throw new NotTapLinkGatewayException("Unexpected failure -> " + e.getMessage());
        }
    }

    public void findLocalAddressRoutableToDevice() {
        Enumeration<NetworkInterface> nets = null;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        for (NetworkInterface nif : Collections.list(nets)) {
            // do something with the network interface
            logger.warn("Found address : {}", nif.getInetAddresses());
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
        if (!headers.contains("Server", "LinkTap Gateway")) {
            throw new NotTapLinkGatewayException("Missing header markers");
        }
    }

    private String extractTitle(final String document) throws NotTapLinkGatewayException {
        final int headStart = document.indexOf("<title>");
        if (headStart == -1) {
            throw new NotTapLinkGatewayException("Missing expected title");
        }
        final int headFinish = document.indexOf("</title>", headStart);
        if (headFinish == -1) {
            throw new NotTapLinkGatewayException("Missing expected title");
        }
        return document.substring(headStart + 7, headFinish); // 7 = "<title>"
    }

    private String extractBody(final String document) throws NotTapLinkGatewayException {
        final int bodyStart = document.indexOf("<body>");
        if (bodyStart == -1) {
            throw new NotTapLinkGatewayException("Missing expected body");
        }
        final int bodyFinish = document.indexOf("</body>", bodyStart);
        if (bodyFinish == -1) {
            throw new NotTapLinkGatewayException("Missing expected body");
        }
        return document.substring(bodyStart + 7, bodyFinish); // 7 = "<title>"
    }

    private String extractRetMarker(String body) {
        // <!--#RET-->{...}
        final int index = body.indexOf("<!--#RET-->");
        return (index != -1) ? body.substring(index + 11).trim() : body;
    }

    public String sendRequest(final String hostname, final String requestBody)
            throws NotTapLinkGatewayException, TransientCommunicationIssueException {
        try {
            final Request request = httpClient.POST("http://" + hostname + "/api.shtml");
            request.content(new StringContentProvider(requestBody), "application/json");

            final ContentResponse cr = request.timeout(REQ_TIMEOUT_SECONDS, TimeUnit.SECONDS).send();
            if (HttpURLConnection.HTTP_OK != cr.getStatus()) {
                throw new NotTapLinkGatewayException("Unexpected status code received " + cr.getStatus());
            }
            ValidateHeaders(cr.getHeaders());
            String responseData = cr.getContentAsString();
            String docTitle = extractTitle(responseData);
            if (!docTitle.equals("api"))
                throw new NotTapLinkGatewayException("Not a LinkTap API response");
            responseData = extractRetMarker(extractBody(responseData));
            return responseData;
        } catch (InterruptedException | TimeoutException e) {
            logger.warn("InterruptedException / TimeoutException -> {}", e.getMessage());
            throw new TransientCommunicationIssueException("Comm timeout or interrupted");
        } catch (ExecutionException e) {
            final Throwable t = e.getCause();
            if (t instanceof UnknownHostException) {
                throw new TransientCommunicationIssueException("Could not resolve host");
            } else if (t instanceof SocketTimeoutException) {
                throw new TransientCommunicationIssueException("Could not reach host");
            } else if (t instanceof SSLHandshakeException) {
                logger.warn("Device doesn't support SSL this is not the gateway device -> {}", e.getMessage());
            } else {
                logger.warn("ExecutionException -> {}", e.getMessage());
            }
            throw new NotTapLinkGatewayException("Unexpected failure -> " + e.getMessage());
        }
    }
}

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
import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.BRIDGE_HTTP_API_ENABLED;
import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.BRIDGE_HTTP_API_EP;
import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.BRIDGE_PROP_GW_ID;
import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.BRIDGE_PROP_GW_VER;
import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.BRIDGE_PROP_HW_MODEL;
import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.BRIDGE_PROP_MAC_ADDR;
import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.BRIDGE_PROP_VOL_UNIT;
import static org.openhab.binding.linktap.protocol.http.NotTapLinkGatewayException.*;
import static org.openhab.binding.linktap.protocol.http.TransientCommunicationIssueException.*;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

    private static final int REQ_TIMEOUT_SECONDS = 3;

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
        if (httpClient != null) {
            this.httpClient = httpClient;
        }
    }

    public Map<String, String> getBridgeProperities(final String hostname)
            throws NotTapLinkGatewayException, TransientCommunicationIssueException {
        try {
            final Request request = httpClient.newRequest(URI_HOST_PREFIX + hostname).method(HttpMethod.GET);
            final ContentResponse cr = request.timeout(REQ_TIMEOUT_SECONDS, TimeUnit.SECONDS).send();
            if (HttpURLConnection.HTTP_OK != cr.getStatus()) {
                throw new NotTapLinkGatewayException(UNEXPECTED_STATUS_CODE);
            }
            validateHeaders(cr.getHeaders());
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
            getMdnsEnableArgs(doc);
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
     * @param doc the html document returns from the potential Gateway device
     * @return Map of common props
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

        newProps.put(BRIDGE_PROP_GW_ID, id.split("[-]")[0]);
        newProps.put(BRIDGE_PROP_GW_VER, firmwareVer.split("[_]")[0]);
        newProps.put(BRIDGE_PROP_MAC_ADDR, macAddr);
        newProps.put(BRIDGE_PROP_HW_MODEL, hwModel);

        /*
         * Extract elements based on name markers and attributes
         */
        final boolean httpApiEnabled = doc.getElementsByAttributeValue("name", "htapi").hasAttr("checked");
        final String httpApiEndpoint = doc.getElementsByAttributeValue("name", "URL").attr("value");

        newProps.put(BRIDGE_HTTP_API_ENABLED, String.valueOf(httpApiEnabled));
        newProps.put(BRIDGE_HTTP_API_EP, httpApiEndpoint);

        Optional<Element> vunitSelections = doc.getElementsByAttributeValue("name", "vunit").stream()
                .filter(x -> x.hasAttr("checked")).findFirst();
        if (vunitSelections.isPresent()) {
            switch (vunitSelections.get().attr("value")) {
                case "0":
                    newProps.put(BRIDGE_PROP_VOL_UNIT, "L");
                    break;
                case "1":
                    newProps.put(BRIDGE_PROP_VOL_UNIT, "gal");
                    break;
            }
        }

        return newProps;
    }

    public Optional<Element> getSection(final Document doc, final String title) {
        final Elements thead = doc.getElementsByTag("thead");
        Optional<Element> element = thead.stream()
                .filter(x -> x.hasText() && x.text().toLowerCase().contains(title.toLowerCase())).findFirst();
        if (element.isPresent()) {
            return Optional.of(element.get().parent());
        }
        return Optional.empty();
    }

    public String getUriInputArg(final Element el) {
        final StringBuilder sb = new StringBuilder();
        switch (el.attr("type")) {
            case "checkbox":
                sb.append(el.attr("name"));
                sb.append("=");
                if (el.hasAttr("checked")) {
                    sb.append(el.attr("value"));
                } else {
                    sb.append("0");
                }
                break;
            case "radio":
                if (el.hasAttr("checked")) {
                    sb.append(el.attr("name"));
                    sb.append("=");
                    sb.append(el.attr("value"));
                }
                break;
            case "text":
                sb.append(el.attr("name"));
                sb.append("=");
                sb.append(URLDecoder.decode(el.attr("value"), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    public String getMdnsEnableArgs(final Document doc) {
        final Optional<Element> miscSection = getSection(doc, "Misc settings");
        StringBuilder sb = new StringBuilder();

        if (!miscSection.isPresent()) {
            return sb.toString();
        }
        final Elements inputs = miscSection.get().getElementsByTag("input");
        for (int i = 0; i < inputs.size(); ++i) {
            final String val = getUriInputArg(inputs.get(i));
            if (!val.isEmpty() && !sb.isEmpty()) {
                sb.append("&");
            }
            sb.append(val);
        }
        // Change the mdns flag to true
        {
            final int mdnsIdx = sb.indexOf("mdns=0");
            if (mdnsIdx != -1) {
                sb.replace(mdnsIdx, mdnsIdx + 6, "mdns=1");
                return sb.toString();
            }
        }

        return "";
    }

    public String getLocalHttpApiArgs(final Document doc, final String targetServer) {
        final Optional<Element> localHttpApiSection = getSection(doc, "Local HTTP API settings");
        StringBuilder sb = new StringBuilder();

        if (!localHttpApiSection.isPresent()) {
            return sb.toString();
        }
        final Elements inputs = localHttpApiSection.get().getElementsByTag("input");
        for (int i = 0; i < inputs.size(); ++i) {
            final String val = getUriInputArg(inputs.get(i));
            if (!val.isEmpty() && !sb.isEmpty()) {
                sb.append("&");
            }
            sb.append(val);
        }

        // Change the enable Local HTTP API flag to true
        final int enableApiIdx = sb.indexOf("htapi=0");
        if (enableApiIdx != -1) {
            sb.replace(enableApiIdx, enableApiIdx + 7, "htapi=1");
        }

        final int urlApiMarker = sb.indexOf("URL=");
        boolean updatedUri = false;
        if (urlApiMarker != -1) {
            final int nextArg = sb.indexOf("&", urlApiMarker);
            String urlArg = (nextArg == -1) ? sb.substring(urlApiMarker + 4) : sb.substring(urlApiMarker + 4, nextArg);
            logger.trace("Found existing HTTP URL Server : {}", urlArg);
            if (!urlArg.equals(targetServer)) {
                updatedUri = true;
                sb.replace(urlApiMarker, urlApiMarker + urlArg.length() + 4,
                        "URL=" + URLEncoder.encode(targetServer, StandardCharsets.UTF_8));
            }
        }

        if (enableApiIdx != -1 || updatedUri) {
            return sb.toString();
        }

        return "";
    }

    public boolean configureBridge(final @Nullable String hostname, final Optional<Boolean> mdnsEnable,
            final Optional<String> localServer)
            throws NotTapLinkGatewayException, TransientCommunicationIssueException {
        try {
            if (hostname == null) {
                throw new TransientCommunicationIssueException("Hostname invalid - null");
            }
            final Request request = httpClient.newRequest(URI_HOST_PREFIX + hostname).method(HttpMethod.GET);
            final ContentResponse cr = request.timeout(REQ_TIMEOUT_SECONDS, TimeUnit.SECONDS).send();
            if (HttpURLConnection.HTTP_OK != cr.getStatus()) {
                throw new NotTapLinkGatewayException(UNEXPECTED_STATUS_CODE);
            }
            validateHeaders(cr.getHeaders());
            final String responseData = cr.getContentAsString();
            final Document doc = Jsoup.parse(responseData);

            switch (doc.title()) {
                case TITLE_API_CONFIG_PAGE:
                    break;
                case TITLE_API_LOGIN_PAGE:
                    return false;
                default:
                    throw new NotTapLinkGatewayException(MISSING_SERVER_TITLE);
            }
            // Send the GET request to configure mdns if it's not enabled
            boolean rebootReq = false;
            if (mdnsEnable.isPresent() && mdnsEnable.get()) {
                logger.trace("Enabling mdns server on gateway");
                String mdnsEnableReqStr = this.getMdnsEnableArgs(doc);
                if (!mdnsEnableReqStr.isEmpty()) {
                    logger.debug("Updating mdns server settings on gateway");
                    final Request mdnsRequest = httpClient
                            .newRequest(URI_HOST_PREFIX + hostname + "/index.shtml?flag=4&" + mdnsEnableReqStr)
                            .method(HttpMethod.GET);
                    final ContentResponse mdnsCr = mdnsRequest.timeout(REQ_TIMEOUT_SECONDS, TimeUnit.SECONDS).send();
                    if (HttpURLConnection.HTTP_OK != mdnsCr.getStatus()) {
                        throw new NotTapLinkGatewayException(UNEXPECTED_STATUS_CODE);
                    }
                    rebootReq = true;
                }
            }

            if (localServer.isPresent() && !localServer.get().isBlank()) {
                logger.trace("Setting Local HTTP Api on gateway");
                String localHttpApiReqStr = this.getLocalHttpApiArgs(doc, localServer.get());
                if (!localHttpApiReqStr.isEmpty()) {
                    logger.debug("Updating Local HTTP API server settings on gateway");
                    final Request lhttpApiRequest = httpClient
                            .newRequest(URI_HOST_PREFIX + hostname + "/index.shtml?flag=5&" + localHttpApiReqStr)
                            .method(HttpMethod.GET);
                    final ContentResponse mdnsCr = lhttpApiRequest.timeout(REQ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .send();
                    if (HttpURLConnection.HTTP_OK != mdnsCr.getStatus()) {
                        throw new NotTapLinkGatewayException(UNEXPECTED_STATUS_CODE);
                    }
                    rebootReq = true;
                }
            }

            if (rebootReq) {
                logger.debug("Rebooting gateway to apply new settings");
                final Request restartReq = httpClient.newRequest(URI_HOST_PREFIX + hostname + "/index.shtml?flag=0")
                        .method(HttpMethod.GET);
                final ContentResponse mdnsCr = restartReq.timeout(REQ_TIMEOUT_SECONDS, TimeUnit.SECONDS).send();
                if (HttpURLConnection.HTTP_OK != mdnsCr.getStatus()) {
                    throw new NotTapLinkGatewayException(UNEXPECTED_STATUS_CODE);
                }
            }

            return rebootReq;

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
            validateHeaders(cr.getHeaders());
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
    private void validateHeaders(final HttpFields headers) throws NotTapLinkGatewayException {
        if (!headers.contains(HEADER_SERVER, HEADER_GW_SERVER_NAME)) {
            throw new NotTapLinkGatewayException(HEADERS_MISSING);
        }
    }

    public String sendRequest(final String hostname, final String requestBody)
            throws NotTapLinkGatewayException, TransientCommunicationIssueException {
        try {
            final InetAddress address = InetAddress.getByName(hostname);
            logger.trace("API Endpoint: {}", URI_HOST_PREFIX + address.getHostAddress() + "/api.shtml");
            final Request request = httpClient.POST(URI_HOST_PREFIX + address.getHostAddress() + "/api.shtml");
            request.content(new StringContentProvider(requestBody), APPLICATION_JSON_TYPE.toString());

            final ContentResponse cr = request.timeout(REQ_TIMEOUT_SECONDS, TimeUnit.SECONDS).send();
            if (HttpURLConnection.HTTP_OK != cr.getStatus()) {
                throw new NotTapLinkGatewayException(UNEXPECTED_STATUS_CODE);
            }
            validateHeaders(cr.getHeaders());
            String responseData = cr.getContentAsString();
            final Document doc = Jsoup.parse(responseData);
            final String docTitle = doc.title();
            if (!docTitle.equals(TITLE_API_RESPONSE)) {
                throw new NotTapLinkGatewayException(MISSING_API_TITLE);
            }
            responseData = doc.body().text();
            return responseData;
        } catch (InterruptedException | TimeoutException | UnknownHostException e) {
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

    public static final String URI_SCHEME = "http";
    public static final String URI_HOST_PREFIX = URI_SCHEME + "://";

    /**
     * Headers
     */
    public static final String HEADER_SERVER = "Server";
    public static final String HEADER_GW_SERVER_NAME = "LinkTap Gateway";

    /**
     * HTML title field mappings to use cases
     */
    private static final String TITLE_API_RESPONSE = "api";
    private static final String TITLE_API_CONFIG_PAGE = "LinkTap Gateway";
    private static final String TITLE_API_LOGIN_PAGE = "LinkTap Gateway Login";

    /**
     * Field names for form submission API's
     */
    private static final String FIELD_ADMIN_USER = "admin";
    private static final String FIELD_ADMIN_USER_PWD = "adminpwd";
}

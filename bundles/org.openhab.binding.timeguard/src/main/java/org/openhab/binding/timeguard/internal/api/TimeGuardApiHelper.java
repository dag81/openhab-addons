/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.timeguard.internal.api;

import static org.openhab.binding.timeguard.internal.TimeGuardBindingConstants.loginUrl;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.validation.constraints.NotNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Fields;
import org.openhab.binding.timeguard.internal.TimeGuardBindingConstants;
import org.openhab.binding.timeguard.internal.dto.requests.TimeGuardFormRequest;
import org.openhab.binding.timeguard.internal.dto.requests.TimeGuardLoginCredentials;
import org.openhab.binding.timeguard.internal.dto.requests.TimeGuardRequest;
import org.openhab.binding.timeguard.internal.dto.responses.TimeGuardListDevicesResponse;
import org.openhab.binding.timeguard.internal.dto.responses.TimeGuardLoginResponse;
import org.openhab.binding.timeguard.internal.dto.responses.TimeGuardResponse;
import org.openhab.binding.timeguard.internal.exceptions.AuthenticationException;
import org.openhab.binding.timeguard.internal.exceptions.CommunicationsIssueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class TimeGuardApiHelper {

    private final Logger logger = LoggerFactory.getLogger(TimeGuardApiHelper.class);

    private @NonNullByDefault({}) HttpClient httpClient;

    private volatile TimeGuardLoginResponse.@Nullable User loggedInSession;

    HashMap<String, TimeGuardListDevicesResponse.DeviceData> generatedDeviceLookup = new HashMap<>();

    private Map<String, TimeGuardListDevicesResponse.@NotNull DeviceData> deviceIdLookup;

    public TimeGuardApiHelper() {
        deviceIdLookup = new HashMap<>();
    }

    public Map<String, TimeGuardListDevicesResponse.@NotNull DeviceData> getIdLookupMap() {
        return generatedDeviceLookup;
    }

    /**
     * Sets the httpClient object to be used for API calls to Vesync.
     *
     * @param httpClient the client to be used.
     */
    public void setHttpClient(@Nullable HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void discoverDevices() throws AuthenticationException, CommunicationsIssueException {
        try {

            final String result = reqAuthorized(TimeGuardBindingConstants.deviceListUrl, HttpMethod.GET);

            TimeGuardListDevicesResponse resultsPage = TimeGuardBindingConstants.GSON.fromJson(result,
                    TimeGuardListDevicesResponse.class);

            if (resultsPage != null) {
                for (TimeGuardListDevicesResponse.DeviceData device : resultsPage.message) {
                    logger.debug("Found device_id : {}, name: {}, online: {}", device.deviceId, device.name,
                            device.online);

                    // Update the mac address -> device table
                    generatedDeviceLookup.put(device.deviceId, device);
                }
            }
            deviceIdLookup = Collections.unmodifiableMap(generatedDeviceLookup);
        } catch (final AuthenticationException ae) {
            logger.warn("Failed background device scan : {}", ae.getMessage());
            throw ae;
        } catch (final CommunicationsIssueException aie) {
            logger.warn("Failed background device scan - due to server side error");
            throw aie;
        }
    }

    public String reqAuthorized(final String url, final HttpMethod method)
            throws AuthenticationException, CommunicationsIssueException {
        return reqAuthorized(url, method, null);
    }

    public String reqAuthorized(final String url, final HttpMethod method, final @Nullable TimeGuardRequest requestData)
            throws AuthenticationException, CommunicationsIssueException {

        logger.debug("{} @ {} with content\r\n{}", method.asString(), url, requestData);

        String urlWithCredentials = url;

        try {
            if (loggedInSession == null || loggedInSession.id == null || loggedInSession.token == null) {
                throw new AuthenticationException("User is not logged in");
            }

            // Add user authentication parameters
            if (urlWithCredentials != null) {
                urlWithCredentials = urlWithCredentials.replace("${TG_USER_ID}", loggedInSession.id);
                urlWithCredentials = urlWithCredentials.replace("${TG_TOKEN}", loggedInSession.token);
            }

            if (requestData instanceof TimeGuardFormRequest) {
                TimeGuardFormRequest formReq = (TimeGuardFormRequest) requestData;
                formReq.userId = loggedInSession.id;
                formReq.token = loggedInSession.token;
            }

            if (requestData == null) {
                logger.debug("{} @ {}", method.asString(), urlWithCredentials);
            } else {
                logger.debug("{} @ {} with content\r\n{}", method.asString(), urlWithCredentials,
                        TimeGuardBindingConstants.GSON.toJson(requestData));
            }

            Request request;
            if (requestData instanceof TimeGuardFormRequest) {

                Fields fields = new Fields();

                Class<?> c = requestData.getClass();
                List<Field> allFields = new ArrayList<>();
                allFields = getAllFields(allFields, c);

                // Field[] clsFields = c.getDeclaredFields();

                for (Field field : allFields) {
                    try {
                        if (field.get(requestData) != null) {
                            fields.add(convertToFormName(field.getName()), field.get(requestData).toString());
                        }
                    } catch (IllegalArgumentException e1) {
                    } catch (IllegalAccessException e1) {
                    }
                }

                request = httpClient.newRequest(url).method(method).content(new FormContentProvider(fields));
            } else {
                request = httpClient.newRequest(urlWithCredentials).method(method);

                if (requestData != null) {
                    request.content(new StringContentProvider(TimeGuardBindingConstants.GSON.toJson(requestData)));
                }
                request.header(HttpHeader.USER_AGENT, "okhttp/3.3.1");
                request.header(HttpHeader.ACCEPT,
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
                request.header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate, br");
                request.header(HttpHeader.ACCEPT_LANGUAGE, "en-GB,en-US;q=0.9,en;q=0.8");
            }

            ContentResponse response = request.timeout(5, TimeUnit.SECONDS).send();
            if (response.getStatus() == HttpURLConnection.HTTP_OK) {
                TimeGuardResponse commResponse = TimeGuardBindingConstants.GSON.fromJson(response.getContentAsString(),
                        TimeGuardResponse.class);

                if (commResponse != null && commResponse.status && commResponse.errorCode == 0) {
                    logger.debug("Got OK response {}", response.getContentAsString());
                    return response.getContentAsString();
                } else {
                    logger.debug("Got FAILED response {}", response.getContentAsString());
                    throw new AuthenticationException("Invalid JSON response");
                }
            } else {
                logger.debug("HTTP Response Code: {}", response.getStatus());
                logger.debug("HTTP Response Msg: {}", response.getReason());

                if (response.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR_500) {
                    throw new CommunicationsIssueException("Internal server error 500 - Likely device is off-line");
                }
                throw new AuthenticationException(
                        "HTTP response " + response.getStatus() + " - " + response.getReason());
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new AuthenticationException(e);
        }
    }

    public static List<Field> getAllFields(List<Field> fields, @Nullable Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    public static String convertToFormName(String fieldName) {
        StringBuilder bldr = new StringBuilder(fieldName.length() + 5);
        for (char ch : fieldName.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                bldr.append('_').append(Character.toLowerCase(ch));
            } else {
                bldr.append(ch);
            }
        }
        return bldr.toString();
    }

    public synchronized void login(final @Nullable String username, final @Nullable String password)
            throws AuthenticationException {
        if (username == null || password == null) {
            loggedInSession = null;
            return;
        }
        try {
            loggedInSession = processLogin(username, password).message.user;
        } catch (final AuthenticationException ae) {
            loggedInSession = null;
            throw ae;
        }
    }

    private TimeGuardLoginResponse processLogin(String username, String password) throws AuthenticationException {
        try {
            Request request = httpClient.newRequest(loginUrl).method(HttpMethod.PUT);

            // No headers for login
            request.content(new StringContentProvider(
                    TimeGuardBindingConstants.GSON.toJson(new TimeGuardLoginCredentials(username, password))));

            request.header(HttpHeader.CONTENT_TYPE, "application/json; utf-8");

            ContentResponse response = request.timeout(5, TimeUnit.SECONDS).send();
            if (response.getStatus() == HttpURLConnection.HTTP_OK) {
                TimeGuardLoginResponse loginResponse = TimeGuardBindingConstants.GSON
                        .fromJson(response.getContentAsString(), TimeGuardLoginResponse.class);
                if (loginResponse != null && loginResponse.status) {
                    logger.debug("Login successful");
                    return loginResponse;
                } else {
                    throw new AuthenticationException("Invalid / unexpected JSON response from login");
                }
            } else {
                logger.warn("Login Failed - HTTP Response Code: {} - {}", response.getStatus(), response.getReason());
                throw new AuthenticationException(
                        "HTTP response " + response.getStatus() + " - " + response.getReason());
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new AuthenticationException(e);
        }
    }
}

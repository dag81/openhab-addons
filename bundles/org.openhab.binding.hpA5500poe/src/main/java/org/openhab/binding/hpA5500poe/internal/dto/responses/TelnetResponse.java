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
package org.openhab.binding.hpA5500poe.internal.dto.responses;

import java.util.regex.Matcher;

import org.openhab.binding.hpA5500poe.internal.dto.requests.TelnetRequest;

/**
 * The {@link TelnetResponse} is the Java class as a DTO to hold login credentials for the Vesync
 * API.
 *
 * @author David Goodyear - Initial contribution
 */
public class TelnetResponse {

    public TelnetResponse(TelnetRequest request, String response, boolean nonErrorResponse) {
        this.request = request;
        this.response = response;
        this.nonErrorResponse = nonErrorResponse;
    }

    public String getResponsePayload() {
        String extractedResponse = this.response;

        if (extractedResponse == null) {
            return null;
        }
        if (request != null) {
            // Remove the command and any text prior to it
            if (request.commandToSend != null) {
                int commandPosition = extractedResponse.indexOf(request.commandToSend);
                if (commandPosition != -1) {
                    extractedResponse = extractedResponse.substring(commandPosition + request.commandToSend.length());
                }
            }

            // We have markers potentially to remove
            if (request.completeMatchPattern != null) {
                Matcher matcher = request.completeMatchPattern.matcher(extractedResponse);
                if (matcher.find()) {
                    extractedResponse = extractedResponse.substring(0,
                            extractedResponse.length() - matcher.group(1).length());
                }
            }
        }
        return extractedResponse.trim();
    }

    public TelnetRequest request = null;
    public String response = null;
    public boolean nonErrorResponse = true;
}

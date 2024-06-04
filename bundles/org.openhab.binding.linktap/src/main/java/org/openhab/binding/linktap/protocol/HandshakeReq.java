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
package org.openhab.binding.linktap.protocol;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link HandshakeReq} is a handshake from the Gateway.
 *
 * @provides App: Gateway ID, Firmware revision, Registered / Addded Endpoint Device ID List
 * @response Gw: Expects response of HandshakeResp, to inform the Gateway of the current local Date and Time
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class HandshakeReq extends TLGatewayFrame {

    public HandshakeReq() {
    }

    /**
     * Defines the firmware version identifier.
     */
    @SerializedName("ver")
    public String version = EMPTY_STRING;

    /**
     * Defines the endpoint devices added / registered to the Gateway.
     * Limited to the first 16 digits and letters of the Device ID
     */
    @SerializedName("end_dev")
    public String[] endDevices = EMPTY_STRING_ARRAY;

    public boolean isValid() {
        if (!super.isValid())
            return false;

        if (version.length() == 0)
            return false;

        for (String ed : endDevices) {
            if (!deviceIdPattern.matcher(ed).matches()) {
                return false;
            }
        }
        return true;
    }
}

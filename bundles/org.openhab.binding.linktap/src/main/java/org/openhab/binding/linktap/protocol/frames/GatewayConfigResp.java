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
package org.openhab.binding.linktap.protocol.frames;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The {@link HandshakeResp} informs the Gateway of the current date, time and weekday in response to
 * a HandshakeReq Frame.
 *
 * @provides Gw: Expects response of HandshakeResp, to inform the Gateway of the current local Date and Time
 * @replyTo HandshakeReq
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class GatewayConfigResp extends HandshakeReq {

    public GatewayConfigResp() {
    }

    /**
     * Defines the units of measurement for volume
     * L = Litres
     * gal = Gallon
     */
    @SerializedName("vol_unit")
    @Expose
    public String volumeUnit = EMPTY_STRING;

    /**
     * Defines the names assigned to each Endpoint device.
     */
    @SerializedName("dev_name")
    @Expose
    public String[] deviceNames = EMPTY_STRING_ARRAY;

    public String isValid() {
        final String superRst = super.isValid();
        if (!superRst.isEmpty()) {
            return superRst;
        }

        if (deviceNames.length != endDevices.length) {
            return "DeviceNames != EndDevices length";
        }

        return EMPTY_STRING;
    }

    /**
     * Unit Volume - Unit tag for gallons
     */
    public static final String UNIT_VOL_GALLON = "gal";

    /**
     * Unit Volume - Unit tag for litres
     */
    public static final String UNIT_VOL_LITRES = "L";
}

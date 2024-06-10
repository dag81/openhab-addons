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
 * The {@link EndpointDeviceResponse} defines the response from the Gateway which includes
 * the targetted endpoint device ID.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class EndpointDeviceResponse extends GatewayDeviceResponse {

    public EndpointDeviceResponse() {
    }

    /**
     * Defines the Endpoint Device ID the return value is for
     */
    @SerializedName("dev_id")
    @Expose
    public String deviceId = EMPTY_STRING;

    public boolean isValid() {
        if (!super.isValid())
            return false;

        if (!deviceIdPattern.matcher(deviceId).matches()) {
            return false;
        }

        return true;
    }
}

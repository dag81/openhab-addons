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
 * The {@link GatewayDeviceResponse} defines the response from the Gateway when a status
 * is given about the state of the requested command.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class GatewayDeviceResponse extends TLGatewayFrame {

    public GatewayDeviceResponse() {
    }

    /**
     * Defines the processing result from the gateway
     */
    @SerializedName("ret")
    public int returnValue = DEFAULT_INT;

    public String getReturnValueDesc() {
        if (returnValue < 0 || returnValue > returnValueDesc.length - 1)
            return "Unknown return value";

        return returnValueDesc[returnValue];
    }

    public boolean isSuccess() {
        return returnValue == 0;
    }

    public boolean isRetryableError() {
        switch (returnValue) {
            case 7: // Conflict with watering plan
            case 6: // Gateway internal error
                return true;
            default:
                return false;
        }
    }

    public static final String[] returnValueDesc = new String[] { "Success", "Message format error",
            "CMD message not supported", "Gateway ID not matched", "End device ID error", "End device ID not found",
            "Gateway internal error", "Conflict with watering plan", "Gateway busy", "Bad parameter in message" };

    public boolean isValid() {
        if (!super.isValid())
            return false;

        if (returnValue < 0 || returnValue > returnValueDesc.length - 1)
            return false;

        return true;
    }
}

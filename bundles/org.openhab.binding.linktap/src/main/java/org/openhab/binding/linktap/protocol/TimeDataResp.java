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
 * The {@link TimeDataResp} defines a response that defines the time, date and weekday
 * from the gateway.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class TimeDataResp extends HandshakeResp {

    public TimeDataResp() {
    }

    /**
     * Defines the processing result from the gateway
     */
    @SerializedName("ret")
    public int returnValue = DEFAULT_INT;

    public String getReturnValueDesc() {
        if (returnValue < 0 || returnValue > GatewayDeviceResponse.returnValueDesc.length - 1)
            return "Unknown return value";

        return GatewayDeviceResponse.returnValueDesc[returnValue];
    }

    public boolean isValid() {
        if (!super.isValid())
            return false;

        if (returnValue < 0 || returnValue > GatewayDeviceResponse.returnValueDesc.length - 1)
            return false;

        return true;
    }
}

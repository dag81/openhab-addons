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
 * The {@link GatewayEndDevListReq} is a reusable frame used for multiple commands where a device endpoint list is included.
 *
 * @provides: Endpoint Device ID List
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class GatewayEndDevListReq extends TLGatewayFrame {

    public GatewayEndDevListReq() {
    }

    /**
     * Defines the endpoint devices added / registered to the Gateway.
     * Limited to the first 16 digits and letters of the Device ID
     */
    @SerializedName("end_dev")
    public String[] endDevices = EMPTY_STRING_ARRAY;

    public boolean isValid() {
        if (!super.isValid())
            return false;

        for (String ed : endDevices) {
            if (!deviceIdPattern.matcher(ed).matches()) {
                return false;
            }
        }
        return true;
    }
}

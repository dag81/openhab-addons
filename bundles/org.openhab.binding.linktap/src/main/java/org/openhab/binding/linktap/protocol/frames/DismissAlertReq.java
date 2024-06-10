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
 * The {@link DismissAlertReq} defines the request to dismiss alerts from a given device.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class DismissAlertReq extends DeviceCmdReq {

    public DismissAlertReq() {
    }

    /**
     * Defines the alert type the dismiss is for.
     */
    @SerializedName("alert")
    @Expose
    public int alert = DEFAULT_INT;

    public boolean isValid() {
        if (!super.isValid())
            return false;

        if (alert < ALERT_TYPES_ALL || alert > ALERT_UNEXPECTED_LOW_FLOW)
            return false;

        return true;
    }

    /**
     * Alert - 0. All types of alert
     */
    public static int ALERT_TYPES_ALL = 0;

    /**
     * Alert - 1. Device fall alert
     */
    public static int ALERT_DEVICE_FALL = 1;

    /**
     * Alert - 2. Valve shutdown failure alert
     */
    public static int ALERT_VALVE_SHUTDOWN_FAIL = 2;

    /**
     * Alert - 3. Water cut-off alert
     */
    public static int ALERT_WATER_CUTOFF = 3;

    /**
     * Alert - 4. Unusually high flow alert
     */
    public static int ALERT_UNEXPECTED_HIGH_FLOW = 4;

    /**
     * Alert - 5. Unusually low flow alert
     */
    public static int ALERT_UNEXPECTED_LOW_FLOW = 5;
}

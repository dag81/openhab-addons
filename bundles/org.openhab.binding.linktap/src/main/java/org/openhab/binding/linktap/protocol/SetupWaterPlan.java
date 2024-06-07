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

import com.google.gson.annotations.SerializedName;
import org.eclipse.jdt.annotation.NonNullByDefault;

import static org.openhab.binding.linktap.protocol.WaterMeterStatus.OP_MODE_INSTANT;
import static org.openhab.binding.linktap.protocol.WaterMeterStatus.OP_MODE_MONTH;

/**
 * The {@link SetupWaterPlan} defines the request to dismiss alerts from a given device.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public abstract class SetupWaterPlan extends DeviceCmdReq {

    public SetupWaterPlan() {
    }

    /**
     * Defines the unique identifier for the instance of the watering plan
     * that we are sending.
     */
    @SerializedName("plan_sn")
    public int planSerialNo = DEFAULT_INT;

    /**
     * Defines the watering mode which can be:
     * watering mode (1 - Instant Mode, 2 - Calendar mode, 3 - 7 day mode, 4 - Odd-even mode, 5
     * - Interval mode, 6 - Month mode).
     * See OP_MODE_INSTANT....
     */
    @SerializedName("mode")
    public int mode = DEFAULT_INT;

    /**
     * Defines the eco mode options... only used by Instant mode but in all payloads
     * eco: the ECO mode works in a way that the valve opens for X seconds then closes
     * for Y seconds “X, Y, X, Y, …“. in [X, Y], X denotes the Valve ON duration,
     * Y denotes Valve OFF duration. The ECO mode will not be applied if either X or Y is zero.
     */
    protected int[] eco = new int[]{0,0};

    /**
     * Defines the watering plan information for the mode specified
     */
    @SerializedName("sch")
    public WaterSchedule schedule = new WaterSchedule();

    protected class WaterSchedule implements IPayloadValidator {
        @Override
        public boolean isValid() {
            return false;
        }
    }

    public boolean isValid() {
        if (!super.isValid())
            return false;

        if (planSerialNo == DEFAULT_INT)
            return false;

        if (mode < OP_MODE_INSTANT || mode > OP_MODE_MONTH)
            return false;

        if (eco.length != 2)
            return false;

        return schedule.isValid();
    }

}

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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.SerializedName;
import org.eclipse.jdt.annotation.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@link DeviceCmdReq} is a payload representing the current status of the
 * water timer.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class WaterMeterStatus extends GatewayDeviceResponse {

    public WaterMeterStatus() {
    }

    @Override
    public int getRes() {
        if (super.getRes() == -1)
            return 0;
        return super.getRes();
    }

    public static class DeviceStatusClassTypeAdapter implements JsonDeserializer<List<DeviceStatus>> {
        public @Nullable List<DeviceStatus> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) {
            List<DeviceStatus> vals = new ArrayList<>();
            if (json.isJsonArray()) {
                for (JsonElement e : json.getAsJsonArray()) {
                    vals.add(ctx.deserialize(e, DeviceStatus.class));
                }
            } else if (json.isJsonObject()) {
                vals.add(ctx.deserialize(json, DeviceStatus.class));
            } else {
                throw new RuntimeException("Unexpected JSON type: " + json.getClass());
            }
            return vals;
        }
    }

    /**
     * Defines the device stat's for each device
     */
    @SerializedName("dev_stat")
    public List<DeviceStatus> deviceStatuses = new ArrayList<DeviceStatus>();
    public class DeviceStatus implements IPayloadValidator {
        /**
         * Defines the targetted device ID
         */
        @SerializedName("dev_id")
        public String deviceId = EMPTY_STRING;

        /**
         * Defines the currently active plan (Operating Mode)
         */
        @SerializedName("plan_mode")
        public int planMode = DEFAULT_INT;

        /**
         * Defines the serial number of the currently active plan
         */
        @SerializedName("plan_sn")
        public int planSerialNo = DEFAULT_INT;

        /**
         * Defines if the water timer is connected to the Gateway
         */
        @SerializedName("is_rf_linked")
        public boolean isRfLinked = false;

        /**
         * Defines the connection of the flow meter
         */
        @SerializedName("is_flm_linked")
        public boolean isFlmLinked = false;

        /**
         * Water timer fall alert status
         */
        @SerializedName("is_fall")
        public boolean isFall = false;

        /**
         * Valve shut-down failure alert status
         */
        @SerializedName("is_broken")
        public boolean isBroken = false;

        /**
         * Water cut-off alert status
         */
        @SerializedName("is_cutoff")
        public boolean isCutoff = false;

        /**
         * Unusually high flow alert status
         */
        @SerializedName("is_leak")
        public boolean isLeak = false;

        /**
         * Unusually low flow alert status
         */
        @SerializedName("is_clog")
        public boolean isClog = false;

        /**
         * Water timer signal reception level
         */
        @SerializedName("signal")
        public int signal = DEFAULT_INT;

        /**
         * Water timer battery level
         */
        @SerializedName("battery")
        public int battery = DEFAULT_INT;

        /**
         * Defines the lock in operation
         */
        @SerializedName("child_lock")
        public int childLock = DEFAULT_INT;

        /**
         * Is manual watering currently on
         */
        @SerializedName("is_manual_mode")
        public boolean isManualMode = false;

        /**
         * Is watering currently on
         */
        @SerializedName("is_watering")
        public boolean isWatering = false;

        /**
         * When the ECO mode is enabled, the watering duration is divided into multiple "on-off-on-off" segments.
         * If is_final is true,it means current watering belongs to the last segment. If both is_watering and is_final
         * are false,it means that the watering is currently suspended (i.e. in midst of the segments), and there are
         * subsequent watering seqments to be executed.
         */
        @SerializedName("is_final")
        public boolean isFinal = false;

        /**
         * The duration of the current watering cycle in seconds
         */
        @SerializedName("total_duration")
        public int totalDuration = DEFAULT_INT;

        /**
         * The remaining duration of the current watering cycle in seconds
         */
        @SerializedName("remain_duration")
        public int remainDuration = DEFAULT_INT;

        /**
         * The current water flow rate (LPN or GPM)
         */
        @SerializedName("speed")
        public double speed = 0.0d;

        /**
         * The accumulated volume of the current watering cycle (Litre or Gallon)
         */
        @SerializedName("volume")
        public double volume = 0.0d;

        public boolean isValid() {

            if (planMode < 1 || planMode > opModeDesc.length) {
                return false;
            }

            if (signal < 0 || signal > 100)
                return false;

            if (battery < 0 || battery > 100)
                return false;

            if (planSerialNo == DEFAULT_INT)
                return false;

            if (childLock < LockReq.LOCK_UNLOCKED || childLock > LockReq.LOCK_FULL)
                return false;

            if (!deviceIdPattern.matcher(deviceId).matches()) {
                return false;
            }

            return true;
        }
    }

    public boolean isValid() {
        if (!super.isValid())
            return false;

        return true;
    }

    public static final int OP_MODE_INSTANT = 1;

    public static final int OP_MODE_CALENDAR = 2;

    public static final int OP_MODE_WEEK_TIMER = 3;

    public static final int OP_MODE_ODD_EVEN = 4;

    public static final int OP_MODE_INTERVAL = 5;

    public static final int OP_MODE_MONTH = 6;

    public static final String[] opModeDesc = new String[] { "Instant Mode", "Calendar Mode", "7 Day Mode",
            "Odd-Even Mode", "Interval Mode", "Month Mode" };
}

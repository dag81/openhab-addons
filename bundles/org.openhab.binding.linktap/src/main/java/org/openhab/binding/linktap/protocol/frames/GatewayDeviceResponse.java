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
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.Expose;
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
    @Expose(serialize = false, deserialize = true)
    private @Nullable Integer returnValue = null;
    @Expose(serialize = false, deserialize = false)
    private ResultStatus cachedResEnum = ResultStatus.INVALID;

    public ResultStatus getRes() {
        if (cachedResEnum == ResultStatus.INVALID) {
            if (returnValue != null) {
                cachedResEnum = ResultStatus.values()[returnValue.intValue()];
            }
        }
        return cachedResEnum;
    }

    public boolean isSuccess() {
        return ResultStatus.RET_SUCCESS == getRes();
    }

    public boolean isRetryableError() {
        switch (getRes()) {
            case RET_CONFLICT_WATER_PLAN: // Conflict with watering plan
            case RET_GW_INTERNAL_ERR: // Gateway internal error
                return true;
            default:
                return false;
        }
    }

    public boolean isValid() {
        if (!super.isValid())
            return false;

        if (ResultStatus.INVALID == getRes()) {
            return false;
        }

        return true;
    }

    public enum ResultStatus {

        /**
         * RET_SUCCESS (Ordinal 0).
         */
        RET_SUCCESS(0, "Success"),

        /**
         * RET_MESSAGE_FORMAT_ERR (Ordinal 1).
         */
        RET_MESSAGE_FORMAT_ERR(1, "Message format error"),

        /**
         * RET_CMD_NOT_SUPPORTED (Ordinal 2).
         */
        RET_CMD_NOT_SUPPORTED(2, "CMD message not supported"),

        /**
         * RET_GATEWAY_ID_NOT_MATCHED (Ordinal 3).
         */
        RET_GATEWAY_ID_NOT_MATCHED(3, "Gateway ID not matched"),

        /**
         * RET_DEVICE_ID_ERROR (Ordinal 4).
         */
        RET_DEVICE_ID_ERROR(4, "End device ID error"),

        /**
         * RET_DEVICE_NOT_FOUND (Ordinal 5).
         */
        RET_DEVICE_NOT_FOUND(5, "End device ID not found"),

        /**
         * RET_GW_INTERNAL_ERR (Ordinal 6).
         */
        RET_GW_INTERNAL_ERR(6, "Gateway internal error"),

        /**
         * RET_CONFLICT_WATER_PLAN (Ordinal 7).
         */
        RET_CONFLICT_WATER_PLAN(7, "Conflict with watering plan"),

        /**
         * RET_GATEWAY_BUSY (Ordinal 8).
         */
        RET_GATEWAY_BUSY(8, "Gateway busy"),

        /**
         * RET_BAD_PARAMETER (Ordinal 9).
         */
        RET_BAD_PARAMETER(9, "Bad parameter in message"),

        /**
         * INVALID (Ordinal -1).
         */
        INVALID(-1, "Not Provided");

        private final int value;
        private final String description;

        private ResultStatus(final int value, final String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDesc() {
            return description;
        }

        @Override
        public String toString() {
            return String.format("%d - %s", value, description);
        }
    }

    /*
     * public final static int RET_SUCCESS = 0;
     * public final static int RET_MESSAGE_FORMAT_ERR = 1;
     * public final static int RET_CMD_NOT_SUPPORTED = 2;
     * public final static int RET_GATEWAY_ID_NOT_MATCHED = 3;
     * public final static int RET_DEVICE_ID_ERROR = 4;
     * public final static int RET_DEVICE_NOT_FOUND = 5;
     * public final static int RET_GW_INTERNAL_ERR = 6;
     * public final static int RET_CONFLICT_WATER_PLAN = 7;
     * public final static int RET_GATEWAY_BUSY = 8;
     * public final static int RET_BAD_PARAMETER = 9;
     */
}

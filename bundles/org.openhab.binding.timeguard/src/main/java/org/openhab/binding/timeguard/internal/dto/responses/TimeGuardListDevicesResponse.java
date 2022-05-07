/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.timeguard.internal.dto.responses;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TimeGuardListDevicesResponse} is a Java class used as a DTO to hold the TimeGuard's API's login response.
 *
 * @author David Goodyear - Initial contribution
 */
public class TimeGuardListDevicesResponse extends TimeGuardResponse {

    @SerializedName("message")
    public DeviceData[] message;

    public class DeviceData {
        @SerializedName("device_id")
        public String deviceId;
        @SerializedName("name")
        public String name;
        @SerializedName("online")
        public String online;
        @SerializedName("codeversion")
        public String codeVersion;
        @SerializedName("relay")
        public String relay;
        @SerializedName("work_mode")
        public String workMode;
        @SerializedName("advance")
        public String advance;
        @SerializedName("main_relay")
        public MainRelay mainRelay;
    }

    public class MainRelay {
        @SerializedName("loaded")
        public String loaded;
        @SerializedName("work_status")
        public String workStatus;
    }
}

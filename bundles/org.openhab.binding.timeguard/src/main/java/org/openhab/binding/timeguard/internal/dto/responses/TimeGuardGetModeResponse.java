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
 * The {@link TimeGuardGetModeResponse} is a Java class used as a DTO to hold the TimeGuard's API's login response.
 *
 * @author David Goodyear - Initial contribution
 */
public class TimeGuardGetModeResponse extends TimeGuardResponse {

    @SerializedName("message")
    public Message message;

    public class Message {
        @SerializedName("work_mode")
        public String workMode;
        @SerializedName("holiday")
        public Holiday holiday;
    }

    public class Holiday {
        @SerializedName("enable")
        public String enable;
        @SerializedName("end")
        public String end;
        @SerializedName("start")
        public String start;
    }
}

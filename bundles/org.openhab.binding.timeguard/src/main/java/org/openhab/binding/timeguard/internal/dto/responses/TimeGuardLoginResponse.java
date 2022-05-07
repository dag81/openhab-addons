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
 * The {@link TimeGuardLoginResponse} is a Java class used as a DTO to hold the TimeGuard's API's login response.
 *
 * @author David Goodyear - Initial contribution
 */
public class TimeGuardLoginResponse extends TimeGuardResponse {

    @SerializedName("message")
    public Message message;

    public class Message {
        @SerializedName("user")
        public User user;
        @SerializedName("wifi_box")
        public WifiBox[] wifiBox;
    }

    public class User {
        @SerializedName("id")
        public String id;
        @SerializedName("username")
        public String username;
        @SerializedName("email")
        public String email;
        @SerializedName("add_time")
        public String addTime;
        @SerializedName("token")
        public String token;
        @SerializedName("token_add_time")
        public String tokenAddTime;
        @SerializedName("is_sub_account")
        public int isSubAccount;
    }

    public class WifiBox {
        @SerializedName("device_id")
        public String deviceId;
        @SerializedName("name")
        public String name;
        @SerializedName("online")
        public String online;
    }
}

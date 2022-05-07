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
package org.openhab.binding.timeguard.internal.dto.requests;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link TimeGuardLoginCredentials} is the Java class as a DTO to hold login credentials for the Vesync
 * API.
 *
 * @author David Goodyear - Initial contribution
 */
public class TimeGuardLoginCredentials {

    @SerializedName("username")
    public String username;
    @SerializedName("password")
    public String password;

    public TimeGuardLoginCredentials() {
        super();
    }

    public TimeGuardLoginCredentials(String username, String password) {
        this();
        this.username = username;
        this.password = password;
    }
}

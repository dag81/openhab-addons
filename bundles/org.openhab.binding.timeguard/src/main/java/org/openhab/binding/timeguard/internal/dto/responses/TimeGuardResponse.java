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
 * The {@link TimeGuardResponse} is a Java class used as a DTO to hold the TimeGuard's API's response.
 *
 * @author David Goodyear - Initial contribution
 */
public class TimeGuardResponse {

    @SerializedName("status")
    public boolean status;

    @SerializedName("error_code")
    public int errorCode;
}

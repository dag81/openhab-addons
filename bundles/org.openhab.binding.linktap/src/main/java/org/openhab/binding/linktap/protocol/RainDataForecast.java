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
 * The {@link DismissAlertReq} defines the payload to represent rainfall forecast data.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class RainDataForecast extends RainData {

    public RainDataForecast() {
    }

    /**
     * Defines the effective time relevant for the rainfall data returned.
     * Minimum value is 1
     */
    @SerializedName("valid_duration")
    public int validDuration = DEFAULT_INT;

    public boolean isValid() {
        if (!super.isValid())
            return false;

        if (validDuration < 1)
            return false;

        return true;
    }
}

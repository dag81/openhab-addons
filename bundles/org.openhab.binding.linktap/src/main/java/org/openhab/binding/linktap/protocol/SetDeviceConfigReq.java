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
 * The {@link SetDeviceConfigReq} sets the configuration parameter specified for a particular device.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class SetDeviceConfigReq extends DeviceCmdReq {

    public SetDeviceConfigReq() {
    }

    /**
     * The value to send for the given tag
     */
    @SerializedName("value")
    public int value = 0;

    /**
     * The tag that the value suppied is to be used for
     */
    @SerializedName("tag")
    public String tag = EMPTY_STRING;

    public boolean isValid() {
        if (!super.isValid())
            return false;

        return true;
    }
}

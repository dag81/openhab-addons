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
 * The {@link AlertStateReq} defines the request to enable or disable alerts from a given device.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class AlertStateReq extends DismissAlertReq {

    public AlertStateReq() {
    }

    /**
     * Defines the alert type to be enabled or disabled
     */
    @SerializedName("enable")
    public boolean enable;

    public boolean isValid() {
        if (!super.isValid())
            return false;

        return true;
    }
}

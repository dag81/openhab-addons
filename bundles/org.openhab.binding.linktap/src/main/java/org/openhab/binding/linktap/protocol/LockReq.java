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
 * The {@link LockReq} defines the request to dismiss alerts from a given device.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class LockReq extends DeviceCmdReq {

    public LockReq() {
    }

    /**
     * Defines the lock type to reqest
     */
    @SerializedName("lock")
    public int lock = DEFAULT_INT;

    public boolean isValid() {
        if (!super.isValid())
            return false;

        if (lock < LOCK_UNLOCKED || lock > LOCK_FULL)
            return false;

        return true;
    }

    /**
     * Lock - 0. Device is unlocked
     */
    public static int LOCK_UNLOCKED = 0;

    /**
     * Lock - 1. Partially locked
     */
    public static int LOCK_PARTIALLY = 1;

    /**
     * Lock - 2. Completely locked
     */
    public static int LOCK_FULL = 2;
}

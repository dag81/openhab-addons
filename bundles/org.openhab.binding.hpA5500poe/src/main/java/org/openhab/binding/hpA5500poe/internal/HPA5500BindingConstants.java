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
package org.openhab.binding.hpA5500poe.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link HPA5500BindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author david.goodyear@gmail.com - Initial contribution
 */
@NonNullByDefault
public class HPA5500BindingConstants {

    private static final String BINDING_ID = "hpA5500poe";

    public static final String DEVICE_PROP_DEVICE_ID = "Device Id";
    public static final String DEVICE_PROP_CODE_VERSION = "Code Version";
    public static final String DEVICE_PROP_NAME = "Device Name";

    public static final String DEVICE_PROP_CONFIG_DEVICE_NAME = "deviceName";
    public static final String DEVICE_PROP_CONFIG_DEVICE_ID = "deviceId";

    // List of all Channel ids
    public static final String CHANNEL_DEVICE_WORK_MODE = "workMode";
    public static final String EMPTY_STRING = "";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_POE_PORT_DEVICE = new ThingTypeUID(BINDING_ID, "poePort");

    public static final String DEVICE_CHANNEL_POE_PORT_ENABLED = "poePortPowerEnabled";
    public static final String DEVICE_CHANNEL_POE_PORT_OP_STATE = "poePortOperatingState";
    public static final String DEVICE_CHANNEL_POE_PORT_POWER_PRIORITY = "poePortPowerPriority";
    public static final String DEVICE_CHANNEL_POE_PORT_IEEE_CLASS = "poePortIEEEClass";
    public static final String DEVICE_CHANNEL_POE_PORT_DETECTION_STATUS = "poePortDetectionStatus";
    public static final String DEVICE_CHANNEL_POE_PORT_POWER_MODE = "poePortPowerMode";
    public static final String DEVICE_CHANNEL_POE_PORT_CURRENT_POWER = "poePortCurrentPower";
    public static final String DEVICE_CHANNEL_POE_PORT_AVERAGE_POWER = "poePortAveragePower";
    public static final String DEVICE_CHANNEL_POE_PORT_PEAK_POWER = "poePortPeakPower";
    public static final String DEVICE_CHANNEL_POE_PORT_MAX_POWER = "poePortMaxPower";
    public static final String DEVICE_CHANNEL_POE_PORT_VOLTAGE = "poePortVoltage";
    public static final String DEVICE_CHANNEL_POE_PORT_CURRENT = "poePortCurrent";
}

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
package org.openhab.binding.linktap.internal;

import java.lang.reflect.Type;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.linktap.protocol.frames.WaterMeterStatus;
import org.openhab.core.thing.ThingTypeUID;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

/**
 * The {@link LinkTapBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class LinkTapBindingConstants {

    final static Type deviceStatusClassListType = new TypeToken<List<WaterMeterStatus.DeviceStatus>>() {
    }.getType();

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(deviceStatusClassListType, new WaterMeterStatus.DeviceStatusClassTypeAdapter())
            .excludeFieldsWithoutExposeAnnotation().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .disableHtmlEscaping().create();

    private static final String BINDING_ID = "linktap";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_SAMPLE = new ThingTypeUID(BINDING_ID, "sample");
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "LinkTapBridge");

    // List of all Channel ids
    public static final String CHANNEL_1 = "channel1";

    public static final String BRIDGE_PROP_GW_ID = "Gateway Id";
    public static final String BRIDGE_PROP_HW_MODEL = "Hardware Model";
    public static final String BRIDGE_PROP_GW_VER = "Version";
    public static final String BRIDGE_PROP_MAC_ADDR = "MAC Address";
    public static final String BRIDGE_HTTP_API_ENABLED = "HTTP API Enabled";
    public static final String BRIDGE_HTTP_API_EP = "HTTP API Callback URI";
    public static final String BRIDGE_PROP_VOL_UNIT = "Volume Unit";

    // Property name constants
    public static final String DEVICE_PROP_DEVICE_NAME = "Device Name";
    public static final String DEVICE_PROP_DEVICE_TYPE = "Device Type";
    public static final String DEVICE_PROP_DEVICE_MAC_ID = "MAC Id";
    public static final String DEVICE_PROP_DEVICE_FAMILY = "Device Family";
    public static final String DEVICE_PROP_DEVICE_UUID = "UUID";
}

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
package org.openhab.binding.timeguard.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link TimeGuardBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author david.goodyear@gmail.com - Initial contribution
 */
@NonNullByDefault
public class TimeGuardBindingConstants {

    public static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting()
            .disableHtmlEscaping().serializeNulls().create();

    private static final String BINDING_ID = "timeguard";

    public static final String DEVICE_PROP_DEVICE_ID = "Device Id";
    public static final String DEVICE_PROP_CODE_VERSION = "Code Version";
    public static final String DEVICE_PROP_NAME = "Device Name";

    public static final String DEVICE_PROP_CONFIG_DEVICE_NAME = "deviceName";
    public static final String DEVICE_PROP_CONFIG_DEVICE_ID = "deviceId";

    // List of all Channel ids
    public static final String CHANNEL_DEVICE_WORK_MODE = "workMode";
    public static final String EMPTY_STRING = "";

    public static final String loginUrl = "https://www.cloudwarm.net/TimeGuard/api/Android/v_1/users/login";
    public static final String deviceListUrl = "https://www.cloudwarm.net/TimeGuard/api/Android/v_1/users/wifi_boxes/user_id/${TG_USER_ID}/is_sub_user/0/token/${TG_TOKEN}";
    public static final String deviceInfoUrl = "https://www.cloudwarm.net/TimeGuard/api/Android/v_1/wifi_boxes/data/user_id/${TG_USER_ID}/wifi_box_id/${TG_DEVICE_ID}/token/${TG_TOKEN}";

    public static final String deviceGetMode = "https://www.cloudwarm.net/TimeGuard/api/Android/v_1/wifi_boxes/mode_and_holiday/user_id/${TG_USER_ID}/wifi_box_id/${TG_DEVICE_ID}/token/${TG_TOKEN}";
    public static final String devicePutMode = "https://www.cloudwarm.net/TimeGuard/api/Android/v_1/wifi_boxes/work_mode";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");
}

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
import org.junit.Test;
import org.openhab.binding.linktap.internal.LinkTapBindingConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openhab.binding.linktap.protocol.TLGatewayFrame.CMD_NOTIFICATION_WATERING_SKIPPED;
import static org.openhab.binding.linktap.protocol.TLGatewayFrame.CMD_UPDATE_WATER_TIMER_STATUS;

/**
 * Command 3: Water Timer Status Update Notification
 * Flow 1 --> GW->Broker->App: Notification that there is a update to one or more water timer's
 */
@NonNullByDefault
public class Command3Test {

    /**
     * Command 3: Water Timer Status Update Notification
     * Flow 1 --> GW->Broker->App: Notification that there is a update to one or more water timer's
     * Default format --> object's within an array
     */
    @Test
    public void NotificationTimerUpdateRequest1Decoding() {
        final WaterMeterStatus decoded = LinkTapBindingConstants.GSON.fromJson("{ \"cmd\":3, \"gw_id\":\"CCCCDDDDEEEEFFFF\", \"dev_stat\": [ { \"dev_id\":\"1111222233334444\", \"plan_mode\":2, \"plan_sn\":3134, \"is_rf_linked\":true, \"is_flm_plugin\":false, \"is_fall\":false, \"is_broken\":false, \"is_cutoff\":false, \"is_leak\":false, \"is_clog\":false, \"signal\":100, \"battery\":0, \"child_lock\":0, \"is_manual_mode\":false, \"is_watering\":false, \"is_final\":true, \"total_duration\":0, \"remain_duration\":0, \"speed\":0, \"volume\":0\n} ]\n}",WaterMeterStatus.class);

        assertEquals(CMD_UPDATE_WATER_TIMER_STATUS,decoded.command);
        assertEquals("CCCCDDDDEEEEFFFF",decoded.gatewayId );
        assertTrue(false);
    }

    /**
     * Command 3: Water Timer Status Update Notification
     * Flow 1 --> GW->Broker->App: Notification that there is a update to one water timer
     * Optional format --> object's without array wrapper
     */
    @Test
    public void NotificationTimerUpdateRequest2Decoding() {
        final WaterMeterStatus decoded = LinkTapBindingConstants.GSON.fromJson("{ \"cmd\":3, \"gw_id\":\"CCCCDDDDEEEEFFFF\", \"dev_stat\": { \"dev_id\":\"1111222233334444\", \"plan_mode\":2, \"plan_sn\":3134, \"is_rf_linked\":true, \"is_flm_plugin\":false, \"is_fall\":false, \"is_broken\":false, \"is_cutoff\":false, \"is_leak\":false, \"is_clog\":false, \"signal\":100, \"battery\":0, \"child_lock\":0, \"is_manual_mode\":false, \"is_watering\":false, \"is_final\":true, \"total_duration\":0, \"remain_duration\":0, \"speed\":0, \"volume\":0\n}\n}",WaterMeterStatus.class);

        assertEquals(CMD_UPDATE_WATER_TIMER_STATUS,decoded.command);
        assertEquals("CCCCDDDDEEEEFFFF",decoded.gatewayId );
        assertTrue(false);
    }
}

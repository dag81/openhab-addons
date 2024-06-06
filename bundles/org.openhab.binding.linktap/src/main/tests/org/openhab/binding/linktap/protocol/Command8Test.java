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

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Test;
import org.openhab.binding.linktap.internal.LinkTapBindingConstants;

/**
 * Command 8: Rain Data
 * Flow 1 --> GW->Broker->App: Request for rain data
 * Flow 2 --> App->Broker->GW: Push to update rain data
 */
@NonNullByDefault
public class Command8Test {

    /**
     * Command 8:
     * Flow 1 --> GW->Broker->App: Request for Rain Data Decoding Test
     */
    @Test
    public void RainDataRequestDecoding() {
        final TLGatewayFrame decoded = LinkTapBindingConstants.GSON.fromJson("{ \"cmd\":8, \"gw_id\":\"CCCCDDDDEEEEFFFF\"\n" +
                "}",TLGatewayFrame.class);

        assertEquals(8,decoded.command);
        assertEquals(8,decoded.getCommand());
        assertEquals("CCCCDDDDEEEEFFFF",decoded.gatewayId );
        assertEquals("CCCCDDDDEEEEFFFF",decoded.getGatewayId() );
    }

    /**
     * Command 8:
     * Flow 1 --> GW->Broker->App: Response serialisation test for Rain Data reply
     */
    @Test
    public void RainDataRequestResponseGenerationTest() {
        RainDataForecast forecastReply = new RainDataForecast();
        forecastReply.setCommand(8);
        forecastReply.setGatewayId("CCCCDDDDEEEEFFFF");
        forecastReply.setPastRainfall(2.5);
        forecastReply.setFutureRainfall(6.3);
        forecastReply.setValidDuration(60);

        String encoded = LinkTapBindingConstants.GSON.toJson(forecastReply);

        assertEquals("{\"valid_duration\":60,\"rain\":[2.5,6.3],\"cmd\":8,\"gw_id\":\"CCCCDDDDEEEEFFFF\"}",
                encoded);
    }

    /**
     * Command 8:
     * Flow 2 --> App->Broker->GW: Push of Rain Data to Gateway request serialisation
     */
    @Test
    public void RainDataPushGenerationTest() {
        RainDataForecast forecastReply = new RainDataForecast();
        forecastReply.setCommand(8);
        forecastReply.setGatewayId("CCCCDDDDEEEEFFFF");
        forecastReply.setPastRainfall(2.5);
        forecastReply.setFutureRainfall(6.3);
        forecastReply.setValidDuration(60);

        String encoded = LinkTapBindingConstants.GSON.toJson(forecastReply);

        assertEquals("{\"valid_duration\":60,\"rain\":[2.5,6.3],\"cmd\":8,\"gw_id\":\"CCCCDDDDEEEEFFFF\"}",
                encoded);
    }

    /**
     * Command 8:
     * Flow 2 --> App->Broker->GW: Response decoding test for Rain Data Push reply
     */
    @Test
    public void RainDataPushResponseDecoding() {
        final GatewayDeviceResponse decoded = LinkTapBindingConstants.GSON.fromJson("{ \"cmd\":8, \"gw_id\":\"CCCCDDDDEEEEFFFF\", \"ret\":0\n" +
                "}",GatewayDeviceResponse.class);

        assertEquals(8,decoded.command);
        assertEquals(8,decoded.getCommand());
        assertEquals("CCCCDDDDEEEEFFFF",decoded.gatewayId );
        assertEquals("CCCCDDDDEEEEFFFF",decoded.getGatewayId() );
        assertEquals(0,decoded.returnValue);
        assertEquals(0,decoded.getReturnValue());
    }


}

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

import java.util.Arrays;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Command 0: Handshake
 * Flow 1 --> GW->Broker->App: First message after connection with system device mappings
 */
@NonNullByDefault
public class Command0Test {

    /**
     * Command 0:
     * Flow 1 --> GW->Broker->App: First handshake message decoding test
     */
    @Test
    public void HandshakeRequestDecoding() {
        final HandshakeReq decoded = LinkTapBindingConstants.GSON.fromJson("{ \"cmd\":0, \"gw_id\":\"CCCCDDDDEEEEFFFF\", \"ver\":\"G0404172103261024C\", \"end_dev\":[ \"1111222233334444\", \"7777888933336666\", \"2245222233334444\", \"3333999993333555\"\n]\n}",HandshakeReq.class);

        assertEquals(0,decoded.command);
        assertEquals(0,decoded.getCommand());
        assertEquals("CCCCDDDDEEEEFFFF",decoded.gatewayId );
        assertEquals("CCCCDDDDEEEEFFFF",decoded.getGatewayId() );
        assertEquals("G0404172103261024C",decoded.version);
        assertEquals("G0404172103261024C",decoded.getVersion());
        assertEquals(4,decoded.endDevices.length);
        assertTrue(Arrays.asList(decoded.endDevices).contains("1111222233334444"));
        assertTrue(Arrays.asList(decoded.endDevices).contains("7777888933336666"));
        assertTrue(Arrays.asList(decoded.endDevices).contains("2245222233334444"));
        assertTrue(Arrays.asList(decoded.endDevices).contains("3333999993333555"));
    }

    /**
     * Command 0:
     * Flow 1 --> GW->Broker->App: First handshake message response encoding test
     */
    @Test
    public void HandshakeResponseEncoding() {
        HandshakeResp reply = new HandshakeResp();
        reply.setCommand(0);
        reply.setGatewayId("CCCCDDDDEEEEFFFF");
        reply.setDate("20210501");
        reply.setTime("123055");
        reply.setWday(6);

        String encoded = LinkTapBindingConstants.GSON.toJson(reply);

        assertEquals("{\"date\":\"20210501\",\"time\":\"123055\",\"wday\":6,\"cmd\":0,\"gw_id\":\"CCCCDDDDEEEEFFFF\"}",
                encoded);
    }

}

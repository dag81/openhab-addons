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
package org.openhab.binding.linktap.protocol.processors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openhab.binding.linktap.handlers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BridgeManager} is a singleton responsible for routing based on the key (IP address) back to
 * the relevant LinkTapBridgeHandler instance.
 *
 * @author David Goodyear - Initial contribution
 */
public final class BridgeManager {

    private final Logger logger = LoggerFactory.getLogger(BridgeManager.class);

    private static BridgeManager INSTANCE;
    private static final Object InstanceLock = new Object();

    final Map<String, LinkTapBridgeHandler> ipAddrLookup = new ConcurrentHashMap<String, LinkTapBridgeHandler>();

    private BridgeManager() {
    }

    public static BridgeManager getInstance() {
        synchronized (InstanceLock) {
            if (INSTANCE == null) {
                INSTANCE = new BridgeManager();
            }
        }
        return INSTANCE;
    }

    public void registerBridge(final String ipAddress, final LinkTapBridgeHandler bridge) {
        logger.warn("Adding {} -> {}", ipAddress, bridge);
        ipAddrLookup.put(ipAddress, bridge);
        logger.warn("Total mappings is now : {}", ipAddrLookup.size());
    }

    public void deregisterBridge(final String ipAddress, final LinkTapBridgeHandler bridge) {
        logger.warn("Removing {} -> {}", ipAddress, bridge);
        ipAddrLookup.remove(ipAddress);
        logger.warn("Total mappings is now : {}", ipAddrLookup.size());
    }

    public LinkTapBridgeHandler getBridge(final String ipAddress) {
        logger.warn("Locating {}", ipAddress);
        LinkTapBridgeHandler result = ipAddrLookup.get(ipAddress);
        logger.warn("Locating result {} -> {}", ipAddress, result);
        return result;
    }
}

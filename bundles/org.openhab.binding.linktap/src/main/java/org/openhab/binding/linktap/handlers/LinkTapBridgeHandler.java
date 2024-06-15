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
package org.openhab.binding.linktap.handlers;

import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.*;

import java.io.IOException;
import java.net.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.linktap.protocol.frames.HandshakeReq;
import org.openhab.binding.linktap.protocol.frames.TLGatewayFrame;
import org.openhab.binding.linktap.protocol.http.NotTapLinkGatewayException;
import org.openhab.binding.linktap.protocol.http.TransientCommunicationIssueException;
import org.openhab.binding.linktap.protocol.http.WebServerApi;
import org.openhab.binding.linktap.protocol.processors.BridgeManager;
import org.openhab.binding.linktap.protocol.processors.TransactionProcessor;
import org.openhab.binding.linktap.protocol.servers.IHttpClientProvider;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LinkTapBridgeHandler} class defines the handler for a LinkTapHandler
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class LinkTapBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(LinkTapBridgeHandler.class);
    private String bridgeKey = "";
    private IHttpClientProvider httpClientProvider;
    public final BridgeManager manager = BridgeManager.getInstance();

    public LinkTapBridgeHandler(final Bridge bridge, @NotNull IHttpClientProvider httpClientProvider) {
        super(bridge);
        this.httpClientProvider = httpClientProvider;
    }

    @Override
    public void initialize() {
        connect();
    }

    @Override
    public void dispose() {
    }

    private void deregisterBridge() {
        if (!bridgeKey.isEmpty()) {
            manager.deregisterBridge(bridgeKey, this);
            bridgeKey = "";
        }
    }

    public String getHost() {
        final LinkTapBridgeConfiguration config = getConfigAs(LinkTapBridgeConfiguration.class);
        return config.hostname;
    }

    private boolean registerBridge() {
        final WebServerApi api = WebServerApi.getInstance();
        api.setHttpClient(httpClientProvider.getHttpClient());
        final LinkTapBridgeConfiguration config = getConfigAs(LinkTapBridgeConfiguration.class);
        try {
            final InetAddress ip = InetAddress.getByName(new URL("http://" + config.hostname).getHost());
            String newHost = ip.getHostAddress();
            if (!bridgeKey.equals(newHost)) {
                deregisterBridge();
                bridgeKey = newHost;
            }
            manager.registerBridge(bridgeKey, this);
        } catch (MalformedURLException | UnknownHostException e) {
            deregisterBridge();
            return false;
        }
        return true;
    }

    private void connect() {
        final LinkTapBridgeConfiguration config = getConfigAs(LinkTapBridgeConfiguration.class);
        // Check if we can resolve the remote host, if so then it can be mapped back to a bridge handler.
        // If not further communications would fail - so its offline.
        if (!registerBridge()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Hostname / IP cannot be found");
            return;
        }
        final WebServerApi api = WebServerApi.getInstance();
        api.setHttpClient(httpClientProvider.getHttpClient());
        try {
            Map<String, String> bridgeProps;
            bridgeProps = api.getBridgeProperities(bridgeKey);
            if (!bridgeProps.isEmpty()) {
                String protocolId = bridgeProps.get(BRIDGE_PROP_GW_ID).split("[-]")[0];
                logger.warn("Protocol ID = {}", protocolId);
                // Device id = <gw id>['_']<device_id>
                this.updateProperties(bridgeProps);
            } else {
                if (!api.unlockWebInterface(bridgeKey, config.username, config.password)) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Check credentials provided");
                    return;
                }
            }
            updateStatus(ThingStatus.ONLINE);
        } catch (NotTapLinkGatewayException e) {
            deregisterBridge();
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Target Hostname / IP is not a LinkTap Gateway");
        } catch (TransientCommunicationIssueException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Cannot connect to LinkTap Gateway");
        }

        try (Socket socket = new Socket()) {
            try {
                socket.connect(new InetSocketAddress(config.hostname, 80));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.warn("Local address for connectivity is {}", socket.getLocalAddress().getHostAddress());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void runTest() {
        logger.warn("Running Test");
        if (true)
            return;
        final WebServerApi api = WebServerApi.getInstance();
        api.setHttpClient(httpClientProvider.getHttpClient());
        try {
            Map<String, String> bridgeProps = api.getBridgeProperities("10.90.3.2");
            if (!bridgeProps.isEmpty()) {
                logger.warn("10.90.3.2 is unlocked");
                this.updateProperties(bridgeProps);
            } else {
                logger.warn("10.90.3.2 is locked");
            }
        } catch (NotTapLinkGatewayException e) {
            logger.warn("10.90.3.2 is not a Link Tap Gateway!");
        } catch (TransientCommunicationIssueException e) {
            logger.warn("Possible communications issue (auto retry): {}", e.getMessage());
        }

        try {
            if (!api.getBridgeProperities("10.2.2.1").isEmpty()) {
                logger.warn("10.2.2.1 is unlocked");
            } else {
                logger.warn("10.2.2.1 is locked");
            }
        } catch (NotTapLinkGatewayException e) {
            logger.warn("10.2.2.1 is not a Link Tap Gateway!");
        } catch (TransientCommunicationIssueException e) {
            logger.warn("Possible communications issue (auto retry): {}", e.getMessage());
        }

        try {
            if (!api.getBridgeProperities("10.100.43.23").isEmpty()) {
                logger.warn("10.100.43.23 is unlocked");
            } else {
                logger.warn("10.100.43.23 is locked");
            }
        } catch (NotTapLinkGatewayException e) {
            logger.warn("10.100.43.23 is not a Link Tap Gateway!");
        } catch (TransientCommunicationIssueException e) {
            logger.warn("Possible communications issue (auto retry): {}", e.getMessage());
        }

        try {
            if (!api.getBridgeProperities("www.davidgoodyear.com2").isEmpty()) {
                logger.warn("www.davidgoodyear.com2 is unlocked");
            } else {
                logger.warn("www.davidgoodyear.com2 is locked");
            }
        } catch (NotTapLinkGatewayException e) {
            logger.warn("www.davidgoodyear.com2 is not a Link Tap Gateway!");
        } catch (TransientCommunicationIssueException e) {
            logger.warn("Possible communications issue (auto retry): {}", e.getMessage());
        }
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return super.getServices();
    }

    public Map<String, String> getMetadataProperities(final @Nullable HandshakeReq handshake) {
        if (handshake == null) {
            return Map.of();
        }
        final Map<String, String> newProps = new HashMap<>(3);
        newProps.put(BRIDGE_PROP_GW_ID, handshake.gatewayId);
        newProps.put(BRIDGE_PROP_GW_VER, handshake.version);
        newProps.put(BRIDGE_PROP_VOL_UNIT, "?");
        return newProps;
    }

    public String SendRequest(final TLGatewayFrame frame) {
        // Validate the payload is within the expected limits for the device its being sent to
        if (!frame.isValid()) {
            throw new RuntimeException("Payload validation failed - will not send");
        }
        TransactionProcessor tp = TransactionProcessor.getInstance();

        return tp.SendRequest(this, frame);
    }
}

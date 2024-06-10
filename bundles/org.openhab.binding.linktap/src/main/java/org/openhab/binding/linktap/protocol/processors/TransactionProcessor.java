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

import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.GSON;
import static org.openhab.binding.linktap.protocol.frames.GatewayDeviceResponse.*;
import static org.openhab.binding.linktap.protocol.frames.TLGatewayFrame.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.linktap.handlers.*;
import org.openhab.binding.linktap.protocol.frames.GatewayDeviceResponse;
import org.openhab.binding.linktap.protocol.frames.HandshakeResp;
import org.openhab.binding.linktap.protocol.frames.TLGatewayFrame;
import org.openhab.binding.linktap.protocol.http.NotTapLinkGatewayException;
import org.openhab.binding.linktap.protocol.http.TransientCommunicationIssueException;
import org.openhab.binding.linktap.protocol.http.WebServerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TransactionProcessor} is a transaction processor, that each Gateway has an instance of.
 * It is responsible for handling received frames from the Gateway.
 *
 * @author David Goodyear - Initial contribution
 */
public final class TransactionProcessor {

    // The Gateway pushes messages to us, the majority expect a response and are documented as
    // GW->Broker->App. These are sent via a HTTP request to the WebSerlet listening for the payloads.
    // Then we can also send data to the Gateway, these all also typically get a response, and are documented as
    // App->Broker->GW. These are sent via a POST request, to the relevant Gateway.
    // As the Gateway is an embedded device,

    private static TransactionProcessor INSTANCE = new TransactionProcessor();
    private static final Object InstanceLock = new Object();

    private static final WebServerApi api = WebServerApi.getInstance();

    private TransactionProcessor() {
    }

    public static TransactionProcessor getInstance() {
        synchronized (InstanceLock) {
            if (INSTANCE == null) {
                INSTANCE = new TransactionProcessor();
            }
        }
        return INSTANCE;
    }

    private final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);

    private final BridgeManager bridgeIpAddrManager = BridgeManager.getInstance();
    private final BridgeManager bridgeIdManager = BridgeManager.getInstance();

    public void ProcessServletRequest(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp) {
        // Check if we have a bridge for managing the requested device
        LinkTapBridgeHandler bridge = bridgeIpAddrManager.getBridge(req.getRemoteHost());
    }

    public String ProcessGwRequest(final String sourceHost, final int command, final String payload) {
        TLGatewayFrame frame = GSON.fromJson(payload, TLGatewayFrame.class);
        switch (command) {
            case CMD_HANDSHAKE:
                final LocalDateTime currentTime = LocalDateTime.now();
                currentTime.minusDays(1);
                final HandshakeResp resp = new HandshakeResp();
                resp.command = CMD_HANDSHAKE;
                resp.gatewayId = frame.gatewayId;
                resp.wday = currentTime.getDayOfWeek().getValue();
                resp.date = currentTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                resp.time = currentTime.format(DateTimeFormatter.ofPattern("HHmmss"));
                return GSON.toJson(resp);
            case CMD_RAINFALL_DATA:
            case CMD_NOTIFICATION_WATERING_SKIPPED:
            case CMD_DATETIME_SYNC:
                break;
            default:
                logger.warn("Unexpected response frame {} -> {}", command, payload);
                return "";
        }
        return "";
    }

    public String SendRequest(final LinkTapBridgeHandler handler, final TLGatewayFrame request) {
        // We need the hostname from the handler of the bridge
        final String targetHost = handler.getHost();
        logger.warn("Transmitting to GW {} -> Payload {}", targetHost, GSON.toJson(request));

        // Responses can be one of the following types
        try {
            String response = api.sendRequest(targetHost, GSON.toJson(request));
            TLGatewayFrame gatewayFrame = GSON.fromJson(response, TLGatewayFrame.class);

            if (request.command != gatewayFrame.command) {
                logger.warn("Unexpected response from GW (CMD {} != {}", request.command, gatewayFrame.command);
                throw new RuntimeException("Unexpected communication failure");
            }

            switch (gatewayFrame.command) {
                case CMD_ADD_END_DEVICE: // 1
                case CMD_REMOVE_END_DEVICE: // 2
                case CMD_UPDATE_WATER_TIMER_STATUS: // 3
                case CMD_SETUP_WATER_PLAN: // 4
                case CMD_REMOVE_WATER_PLAN: // 5
                case CMD_IMMEDIATE_WATER_START: // 6
                case CMD_IMMEDIATE_WATER_STOP: // 7
                case CMD_RAINFALL_DATA: // 8
                case CMD_ALERT_ENABLEMENT: // 10
                case CMD_ALERT_DISMISS: // 11
                case CMD_LOCKOUT_STATE: // 12
                case CMD_DATETIME_READ: // 14
                case CMD_WIRELESS_CHECK: // 15
                case CMD_GET_CONFIGURATION: // 16
                case CMD_SET_CONFIGURATION: // 17
                case CMD_PAUSE_WATER_PLAN: // 18
                    final GatewayDeviceResponse gdr = GSON.fromJson(response, GatewayDeviceResponse.class);
                    final ResultStatus commResult = gdr.getRes();
                    switch (commResult) {
                        case RET_SUCCESS:
                            logger.warn("Request successfully processed");
                            return response;
                        case RET_MESSAGE_FORMAT_ERR:
                        case RET_BAD_PARAMETER:
                        case RET_CMD_NOT_SUPPORTED:
                            logger.warn("Request issued incorrectly - not supported by the device or format error");
                            break;
                        case RET_DEVICE_ID_ERROR:
                        case RET_DEVICE_NOT_FOUND:
                            logger.warn("Device configuration error - check DEVICE ID in metadata");
                            break;
                        case RET_GATEWAY_ID_NOT_MATCHED:
                            logger.warn("Gateway configuration error - check GATEWAY ID in metadata");
                            break;
                        case RET_GATEWAY_BUSY:
                        case RET_GW_INTERNAL_ERR:
                            logger.warn("The request can be re-tried");
                            break;
                        case RET_CONFLICT_WATER_PLAN:
                            logger.warn("Gateway rejected command due to water plan conflict");
                            break;
                    }
                    break;
                default:
                    logger.warn("Unexpected response frame {} -> {}", gatewayFrame.command, GSON.toJson(request));
                    return "";
            }

            return response;
        } catch (NotTapLinkGatewayException e) {
            throw new RuntimeException(e);
        } catch (TransientCommunicationIssueException e) {
            throw new RuntimeException(e);
        }
    }
}

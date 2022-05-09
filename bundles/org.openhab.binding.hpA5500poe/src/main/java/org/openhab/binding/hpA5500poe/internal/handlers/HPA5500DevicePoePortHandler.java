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
package org.openhab.binding.hpA5500poe.internal.handlers;

import static org.openhab.binding.hpA5500poe.internal.HPA5500BindingConstants.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.hpA5500poe.internal.HPA5500BindingConstants;
import org.openhab.binding.hpA5500poe.internal.HPA5500DevicePoePortConfiguration;
import org.openhab.binding.hpA5500poe.internal.dto.requests.RequestPortPoeStatus;
import org.openhab.binding.hpA5500poe.internal.dto.requests.TelnetRequest;
import org.openhab.binding.hpA5500poe.internal.dto.responses.TelnetResponse;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HPA5500DevicePoePortHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class HPA5500DevicePoePortHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(HPA5500DevicePoePortHandler.class);

    private @Nullable RequestPortPoeStatus reqStatusMsg = null;

    @Override
    public void channelLinked(ChannelUID channelUID) {
        super.channelLinked(channelUID);

        scheduler.execute(this::pollForUpdate);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return super.getServices();
    }

    private @Nullable BridgeHandler getBridgeHandler() {
        Bridge bridgeRef = getBridge();
        if (bridgeRef == null) {
            return null;
        } else {
            return bridgeRef.getHandler();
        }
    }

    public HPA5500DevicePoePortHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        final HPA5500DevicePoePortConfiguration config = getConfigAs(HPA5500DevicePoePortConfiguration.class);
        reqStatusMsg = null;
        if (config.portNumber < 1 || config.portNumber > 48) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Check port number");
        } else {
            reqStatusMsg = new RequestPortPoeStatus(config.portNumber);
            updateStatus(ThingStatus.ONLINE);
            scheduler.schedule(this::pollForUpdate, 3, TimeUnit.SECONDS);

            HPA5500BridgeHandler bridgeRef = (HPA5500BridgeHandler) getBridgeHandler();
            if (bridgeRef != null) {
                bridgeRef.registerPoePort(this);
            }
        }
    }

    @Override
    public void dispose() {
        HPA5500BridgeHandler bridgeRef = (HPA5500BridgeHandler) getBridgeHandler();
        if (bridgeRef != null) {
            bridgeRef.unregisterPoePort(this);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    public void pollForUpdate() {

        final TelnetRequest req = this.reqStatusMsg;
        if (req == null) {
            logger.error("Skipping poll - request is unknown");
            return;
        }

        HPA5500BridgeHandler bridgeRef = (HPA5500BridgeHandler) getBridgeHandler();

        if (bridgeRef == null || ThingStatus.OFFLINE.equals(bridgeRef.getThing().getStatus())) {
            logger.error("Poll blocked as device / bridge is offline");
            return;
        }

        logger.error("Polling for update now");

        TelnetResponse response = ((HPA5500BridgeHandler) getBridgeHandler()).processRequest(req);
        if (response != null) {
            logger.error("Got response - {}", response.response);
            logger.error("Got response payload - '{}'", response.getResponsePayload());
        }

        final String responseRaw = response.getResponsePayload();
        final String[] responseLines = responseRaw.split("\r\n");
        final HashMap<String, String> kpData = new HashMap<>();
        for (String responseLine : responseLines) {
            String[] fields = responseLine.split("[:]");
            kpData.put(fields[0].trim(), fields.length > 1 ? fields[1].trim() : "");
        }

        updateState(DEVICE_CHANNEL_POE_PORT_ENABLED,
                OnOffType.from(kpData.get("Port Power Enabled").equals("enabled")));
        updateState(DEVICE_CHANNEL_POE_PORT_OP_STATE, OnOffType.from(kpData.get("Port Operating Status").equals("on")));
        updateState(DEVICE_CHANNEL_POE_PORT_POWER_PRIORITY, new StringType(kpData.get("Port Power Priority")));
        String IeeeClassStr = kpData.get("Port IEEE Class");
        if (IeeeClassStr != null) {
            updateState(DEVICE_CHANNEL_POE_PORT_IEEE_CLASS, new DecimalType(Integer.valueOf(IeeeClassStr).intValue()));
        } else {
            updateState(DEVICE_CHANNEL_POE_PORT_IEEE_CLASS, new DecimalType(-1));
        }

        updateState(DEVICE_CHANNEL_POE_PORT_DETECTION_STATUS, new StringType(kpData.get("Port Detection Status")));
        updateState(DEVICE_CHANNEL_POE_PORT_POWER_MODE, new StringType(kpData.get("Port Power Mode")));

        if (kpData.containsKey("Port Current Power")) {
            double value = 0;
            final String currentVal = kpData.get("Port Current Power").split(" ")[0];
            if (currentVal != null) {
                value = Integer.valueOf(currentVal).intValue() / 1000d;
            }
            updateState(DEVICE_CHANNEL_POE_PORT_CURRENT_POWER, new QuantityType<>(value, Units.WATT));
        }

        if (kpData.containsKey("Port Average Power")) {
            double value = 0;
            final String currentVal = kpData.get("Port Average Power").split(" ")[0];
            if (currentVal != null) {
                value = Integer.valueOf(currentVal).intValue() / 1000d;
            }
            updateState(DEVICE_CHANNEL_POE_PORT_AVERAGE_POWER, new QuantityType<>(value, Units.WATT));
        }

        if (kpData.containsKey("Port Voltage")) {
            double value = 0;
            final String currentVal = kpData.get("Port Voltage").split(" ")[0];
            if (currentVal != null) {
                value = Double.valueOf(currentVal).doubleValue();
            }
            updateState(DEVICE_CHANNEL_POE_PORT_VOLTAGE, new QuantityType<>(value, Units.VOLT));
        }

        if (kpData.containsKey("Port Peak Power")) {
            double value = 0;
            final String currentVal = kpData.get("Port Peak Power").split(" ")[0];
            if (currentVal != null) {
                value = Integer.valueOf(currentVal).intValue() / 1000d;
            }
            updateState(DEVICE_CHANNEL_POE_PORT_PEAK_POWER, new QuantityType<>(value, Units.WATT));
        }

        if (kpData.containsKey("Port Max Power")) {
            double value = 0;
            final String currentVal = kpData.get("Port Max Power").split(" ")[0];
            if (currentVal != null) {
                value = Integer.valueOf(currentVal).intValue() / 1000d;
            }
            updateState(DEVICE_CHANNEL_POE_PORT_MAX_POWER, new QuantityType<>(value, Units.WATT));
        }

        if (kpData.containsKey("Port Current")) {
            double value = 0;
            final String currentVal = kpData.get("Port Current").split(" ")[0];
            if (currentVal != null) {
                value = Integer.valueOf(currentVal).intValue() / 1000d;
            }
            updateState(DEVICE_CHANNEL_POE_PORT_CURRENT, new QuantityType<>(value, Units.AMPERE));
        }
    }

    public final String sendCommand(final HttpMethod method, final String url) {

        if (ThingStatus.OFFLINE.equals(this.thing.getStatus())) {
            logger.debug("Command blocked as device is offline");
            return HPA5500BindingConstants.EMPTY_STRING;
        }
        return "";
    }
}

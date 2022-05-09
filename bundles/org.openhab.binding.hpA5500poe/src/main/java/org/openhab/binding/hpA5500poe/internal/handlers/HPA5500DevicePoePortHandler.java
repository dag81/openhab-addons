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
import org.openhab.binding.hpA5500poe.internal.HPA5500DevicePoePortConfiguration;
import org.openhab.binding.hpA5500poe.internal.dto.requests.RequestPortPoeStatus;
import org.openhab.binding.hpA5500poe.internal.dto.requests.TelnetRequest;
import org.openhab.binding.hpA5500poe.internal.dto.requests.TelnetRequestSequence;
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

    private int portNumber;

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
            portNumber = config.portNumber;
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
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        if (portNumber < 1 || portNumber > 48)
            throw new RuntimeException("Invalid port number - cannot dispatch command");
        scheduler.submit(() -> {
            if (command instanceof OnOffType) {
                switch (channelUID.getId()) {
                    case DEVICE_CHANNEL_POE_PORT_ENABLED:
                        TelnetRequestSequence cmdSequence = TelnetRequestSequence.createPoePortSetEnable(portNumber,
                                command.equals(OnOffType.ON));
                        HPA5500BridgeHandler bridgeRef = (HPA5500BridgeHandler) getBridgeHandler();
                        if (bridgeRef == null || ThingStatus.OFFLINE.equals(bridgeRef.getThing().getStatus())) {
                            logger.warn("Command blocked as device / bridge is offline");
                            return;
                        }

                        TelnetResponse tr = bridgeRef.sendRequest(cmdSequence);
                        if (tr != null && tr.nonErrorResponse) {
                            logger.debug("Performing read-back");
                            // The devices need's at least 2 seconds to process the command
                            scheduler.schedule(this::pollForUpdate, 2, TimeUnit.SECONDS);
                        }

                        break;
                }
            }
        });
    }

    public void pollForUpdate() {

        final TelnetRequest req = this.reqStatusMsg;
        if (req == null) {
            logger.error("Skipping poll - request is unknown");
            return;
        }

        HPA5500BridgeHandler bridgeRef = (HPA5500BridgeHandler) getBridgeHandler();

        if (bridgeRef == null || ThingStatus.OFFLINE.equals(bridgeRef.getThing().getStatus())) {
            logger.warn("Poll blocked as device / bridge is offline");
            return;
        }

        TelnetResponse response = ((HPA5500BridgeHandler) getBridgeHandler()).sendRequest(req);
        if (response == null)
            return;

        final String responseRaw = response.getResponsePayload();

        final String[] responseLines = responseRaw.split("\r\n");
        final HashMap<String, String> kpData = new HashMap<>();
        for (String responseLine : responseLines) {
            String[] fields = responseLine.split("[:]");
            kpData.put(fields[0].trim(), fields.length > 1 ? fields[1].trim() : "");
        }

        updateState(DEVICE_CHANNEL_POE_PORT_ENABLED,
                OnOffType.from(kpData.get("Port Power Enabled").equals("enabled")));
        updateState(DEVICE_CHANNEL_POE_PORT_POWER_PRIORITY, new StringType(kpData.get("Port Power Priority")));
        updateState(DEVICE_CHANNEL_POE_PORT_OP_STATE, OnOffType.from(kpData.get("Port Operating Status").equals("on")));
        updateState(DEVICE_CHANNEL_POE_PORT_IEEE_CLASS,
                new DecimalType((int) extractValue(kpData, "Port IEEE Class", 0)));
        updateState(DEVICE_CHANNEL_POE_PORT_DETECTION_STATUS, new StringType(kpData.get("Port Detection Status")));
        updateState(DEVICE_CHANNEL_POE_PORT_POWER_MODE, new StringType(kpData.get("Port Power Mode")));
        updateState(DEVICE_CHANNEL_POE_PORT_CURRENT_POWER,
                new QuantityType<>(extractValue(kpData, "Port Current Power", 0) / 1000d, Units.WATT));
        updateState(DEVICE_CHANNEL_POE_PORT_AVERAGE_POWER,
                new QuantityType<>(extractValue(kpData, "Port Average Power", 0) / 1000d, Units.WATT));
        updateState(DEVICE_CHANNEL_POE_PORT_PEAK_POWER,
                new QuantityType<>(extractValue(kpData, "Port Peak Power", 0) / 1000d, Units.WATT));
        updateState(DEVICE_CHANNEL_POE_PORT_MAX_POWER,
                new QuantityType<>(extractValue(kpData, "Port Max Power", 0) / 1000d, Units.WATT));
        updateState(DEVICE_CHANNEL_POE_PORT_CURRENT,
                new QuantityType<>(extractValue(kpData, "Port Current", 0) / 1000d, Units.AMPERE));
        updateState(DEVICE_CHANNEL_POE_PORT_VOLTAGE,
                new QuantityType<>(extractValue(kpData, "Port Voltage", 0), Units.VOLT));
    }

    private double extractValue(final HashMap<String, String> kpData, final String key, double defaultValue) {
        final String currentVal = kpData.get(key).split(" ")[0];
        if (currentVal != null) {
            defaultValue = Double.valueOf(currentVal).doubleValue();
        }
        return defaultValue;
    }
}

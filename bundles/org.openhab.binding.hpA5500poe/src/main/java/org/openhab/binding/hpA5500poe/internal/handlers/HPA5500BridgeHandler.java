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

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.hpA5500poe.internal.HPA5500BridgeConfiguration;
import org.openhab.binding.hpA5500poe.internal.dto.requests.SerialInterfaceLogin;
import org.openhab.binding.hpA5500poe.internal.dto.requests.TelnetRequest;
import org.openhab.binding.hpA5500poe.internal.dto.responses.TelnetResponse;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HPA5500BridgeHandler} is responsible for handling the bridge things created to use the VeSync
 * API. This way, the user credentials may be entered only once.
 *
 * @author David Goodyear - Initial Contribution
 */
@NonNullByDefault
public class HPA5500BridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(HPA5500BridgeHandler.class);

    public @Nullable ScheduledFuture<?> backgroundPoeStatusPollingJob;

    public ThingUID getUID() {
        return thing.getUID();
    }

    TelnetConnection telnetLink = new TelnetConnection();

    public HPA5500BridgeHandler(Bridge bridge) {
        super(bridge);
    }

    CopyOnWriteArraySet<HPA5500DevicePoePortHandler> poePortsToPoll = new CopyOnWriteArraySet<>();

    @Override
    public void initialize() {
        scheduler.submit(() -> startTelnetSession());
    }

    protected void registerPoePort(HPA5500DevicePoePortHandler poePortRepresentation) {
        if (poePortRepresentation != null) {
            poePortsToPoll.add(poePortRepresentation);
        }
    }

    protected void unregisterPoePort(HPA5500DevicePoePortHandler poePortRepresentation) {
        if (poePortRepresentation != null) {
            poePortsToPoll.remove(poePortRepresentation);
        }
    }

    private void pollPoePorts() {
        try {
            logger.error("Running POE Ports poll for {} ports ", poePortsToPoll.size());
            poePortsToPoll.forEach(x -> x.pollForUpdate());
        } catch (Throwable t) {
            logger.error("Error while polling", t);
        }
    }

    private void reconfigurePoePortsPolling(boolean enable) {
        final HPA5500BridgeConfiguration config = getConfigAs(HPA5500BridgeConfiguration.class);
        if (backgroundPoeStatusPollingJob != null) {
            backgroundPoeStatusPollingJob.cancel(false);
        }
        if (enable) {
            backgroundPoeStatusPollingJob = scheduler.scheduleWithFixedDelay(this::pollPoePorts, 2,
                    config.poeStatePollRate, TimeUnit.SECONDS);
        }
    }

    private void startTelnetSession() {
        final HPA5500BridgeConfiguration config = getConfigAs(HPA5500BridgeConfiguration.class);
        try {
            telnetLink.connect(config.ipAddress);
            SerialInterfaceLogin loginSequence = new SerialInterfaceLogin();
            loginSequence.commandToSend = config.telnetPassword;
            TelnetResponse loginResponse = telnetLink.sendRequest(loginSequence);
            if (loginResponse.nonErrorResponse) {
                updateStatus(ThingStatus.ONLINE);
                reconfigurePoePortsPolling(true);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Check telnet serial login");
                reconfigurePoePortsPolling(false);
            }
        } catch (IOException ioe) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Check telnet serial ip/hostname");
            reconfigurePoePortsPolling(false);
        }
    }

    public @Nullable TelnetResponse processRequest(TelnetRequest request) {
        try {
            return telnetLink.sendRequest(request);
        } catch (IOException ioe) {
            startTelnetSession();
            try {
                return telnetLink.sendRequest(request);
            } catch (IOException ioe2) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Lost communications");
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        telnetLink.disconnect();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return super.getServices();
    }

    /*
     * protected void setBackgroundScanInterval(final int seconds) {
     * synchronized (scanConfigLock) {
     * ScheduledFuture<?> job = backgroundDiscoveryPollingJob;
     * if (backgroundScanTime != seconds) {
     * if (seconds > 0) {
     * logger.trace("Scheduling background scanning for new devices / base information every {} seconds",
     * seconds);
     * } else {
     * logger.trace("Disabling background scanning for new devices / base information");
     * }
     * // Cancel the current scan's and re-schedule as required
     * if (job != null && !job.isCancelled()) {
     * job.cancel(true);
     * backgroundDiscoveryPollingJob = null;
     * }
     * if (seconds > 0) {
     * backgroundDiscoveryPollingJob = scheduler.scheduleWithFixedDelay(
     * this::runDeviceScanSequenceNoAuthErrors, seconds, seconds, TimeUnit.SECONDS);
     * }
     * backgroundScanTime = seconds;
     * }
     * }
     * }
     */
}

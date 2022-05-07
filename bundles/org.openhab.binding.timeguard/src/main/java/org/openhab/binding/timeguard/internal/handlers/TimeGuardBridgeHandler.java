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
package org.openhab.binding.timeguard.internal.handlers;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.timeguard.internal.TimeGuardBridgeConfiguration;
import org.openhab.binding.timeguard.internal.api.TimeGuardApiHelper;
import org.openhab.binding.timeguard.internal.discovery.DeviceMetaDataUpdatedHandler;
import org.openhab.binding.timeguard.internal.discovery.TimeGuardDiscoveryService;
import org.openhab.binding.timeguard.internal.dto.requests.TimeGuardRequest;
import org.openhab.binding.timeguard.internal.dto.responses.TimeGuardListDevicesResponse;
import org.openhab.binding.timeguard.internal.exceptions.AuthenticationException;
import org.openhab.binding.timeguard.internal.exceptions.CommunicationsIssueException;
import org.openhab.binding.timeguard.internal.exceptions.DeviceUnknownException;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TimeGuardBridgeHandler} is responsible for handling the bridge things created to use the VeSync
 * API. This way, the user credentials may be entered only once.
 *
 * @author David Goodyear - Initial Contribution
 */
@NonNullByDefault
public class TimeGuardBridgeHandler extends BaseBridgeHandler implements TimeGuardClient {

    private static final int DEFAULT_DEVICE_SCAN_INTERVAL = 600;
    private static final int DEFAULT_DEVICE_SCAN_RECOVERY_INTERVAL = 60;
    private static final int DEFAULT_DEVICE_SCAN_DISABLED = -1;

    private final Logger logger = LoggerFactory.getLogger(TimeGuardBridgeHandler.class);

    private @Nullable ScheduledFuture<?> backgroundDiscoveryPollingJob;

    protected final @NotNull TimeGuardApiHelper api;

    public ThingUID getUID() {
        return thing.getUID();
    }

    public TimeGuardBridgeHandler(Bridge bridge, TimeGuardApiHelper api) {
        super(bridge);
        this.api = api;
    }

    private volatile int backgroundScanTime = -1;
    private final Object scanConfigLock = new Object();

    protected void checkIfIncreaseScanRateRequired() {
        logger.trace("Checking if increased background scanning for new devices / base information is required");
        boolean frequentScanReq = false;
        for (Thing th : getThing().getThings()) {
            ThingHandler handler = th.getHandler();
            if (handler instanceof TimeGuardBaseDeviceHandler) {
                if (((TimeGuardBaseDeviceHandler) handler).requiresMetaDataFrequentUpdates()) {
                    frequentScanReq = true;
                    break;
                }
            }
        }

        if (!frequentScanReq && api.getIdLookupMap().values().stream().anyMatch(x -> "1".equals(x.online) == false)) {
            frequentScanReq = true;
        }

        if (frequentScanReq) {
            setBackgroundScanInterval(DEFAULT_DEVICE_SCAN_RECOVERY_INTERVAL);
        } else {
            setBackgroundScanInterval(DEFAULT_DEVICE_SCAN_INTERVAL);
        }
    }

    protected void setBackgroundScanInterval(final int seconds) {
        synchronized (scanConfigLock) {
            ScheduledFuture<?> job = backgroundDiscoveryPollingJob;
            if (backgroundScanTime != seconds) {
                if (seconds > 0) {
                    logger.trace("Scheduling background scanning for new devices / base information every {} seconds",
                            seconds);
                } else {
                    logger.trace("Disabling background scanning for new devices / base information");
                }
                // Cancel the current scan's and re-schedule as required
                if (job != null && !job.isCancelled()) {
                    job.cancel(true);
                    backgroundDiscoveryPollingJob = null;
                }

                if (seconds > 0) {
                    backgroundDiscoveryPollingJob = scheduler.scheduleWithFixedDelay(
                            this::runDeviceScanSequenceNoAuthErrors, seconds, seconds, TimeUnit.SECONDS);
                }

                backgroundScanTime = seconds;
            }
        }
    }

    public void registerMetaDataUpdatedHandler(DeviceMetaDataUpdatedHandler dmduh) {
        handlers.add(dmduh);
    }

    public void unregisterMetaDataUpdatedHandler(DeviceMetaDataUpdatedHandler dmduh) {
        handlers.remove(dmduh);
    }

    private final CopyOnWriteArrayList<DeviceMetaDataUpdatedHandler> handlers = new CopyOnWriteArrayList<>();

    public void runDeviceScanSequenceNoAuthErrors() {
        try {
            runDeviceScanSequence();
            updateStatus(ThingStatus.ONLINE);
        } catch (AuthenticationException ae) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Check login credentials");
        } catch (CommunicationsIssueException ae) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "TimeGuard server issue's");
        }
    }

    public void runDeviceScanSequence() throws AuthenticationException, CommunicationsIssueException {
        logger.trace("Scanning for new devices / base information now");
        api.discoverDevices();
        handlers.forEach(x -> x.handleMetadataRetrieved(this));
        checkIfIncreaseScanRateRequired();

        this.updateThings();
    }

    public java.util.stream.Stream<TimeGuardListDevicesResponse.@NotNull DeviceData> getDevices() {
        return api.getIdLookupMap().values().stream();
    }

    protected void updateThings() {
        final TimeGuardBridgeConfiguration config = getConfigAs(TimeGuardBridgeConfiguration.class);
        getThing().getThings().forEach((th) -> updateThing(config, th.getHandler()));
    }

    public void updateThing(ThingHandler handler) {
        final TimeGuardBridgeConfiguration config = getConfigAs(TimeGuardBridgeConfiguration.class);
        updateThing(config, handler);
    }

    private void updateThing(TimeGuardBridgeConfiguration config, @Nullable ThingHandler handler) {
        if (handler instanceof TimeGuardBaseDeviceHandler) {
            ((TimeGuardBaseDeviceHandler) handler).updateDeviceMetaData();
            ((TimeGuardBaseDeviceHandler) handler).updateBridgeBasedPolls(config);
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(TimeGuardDiscoveryService.class);
    }

    @Override
    public void initialize() {
        TimeGuardBridgeConfiguration config = getConfigAs(TimeGuardBridgeConfiguration.class);

        scheduler.submit(() -> {
            try {
                api.login(config.username, config.password);
                runDeviceScanSequence();
                updateStatus(ThingStatus.ONLINE);
            } catch (final AuthenticationException ae) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Check login credentials");
            } catch (CommunicationsIssueException ae) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "TimeGuard server issue's");
            }
        });
    }

    @Override
    public void dispose() {
        setBackgroundScanInterval(DEFAULT_DEVICE_SCAN_DISABLED);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.warn("Handling command for VeSync bridge handler.");
    }

    @Override
    public String reqAuthorized(String url, HttpMethod method, @Nullable TimeGuardRequest requestData)
            throws AuthenticationException, DeviceUnknownException, CommunicationsIssueException {
        if (requestData == null) {
            return api.reqAuthorized(url, method);
        } else {
            return api.reqAuthorized(url, method, requestData);
        }
    }
}

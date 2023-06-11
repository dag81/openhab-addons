/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.vesync.internal.handlers;

import static org.openhab.binding.vesync.internal.VeSyncConstants.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.vesync.internal.VeSyncBridgeConfiguration;
import org.openhab.core.cache.ExpiringCache;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.types.Command;

/**
 * The {@link VeSyncDeviceAirFryerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class VeSyncDeviceAirFryerHandler extends VeSyncBaseDeviceHandler {

    public static final String DEV_TYPE_FAMILY_AIR_FRYER = "CS-AF";

    public static final int DEFAULT_AIR_FRYER_POLL_RATE = 30;

    public static final String DEV_FAMILY_CORSORI_3758L = "Corsori 3758L";

    /**
     * As further models are introduced a better pattern match may work, for now explicity match like pyvesync does.
     */
    public static final VeSyncDeviceMetadata COSORI3758L = new VeSyncDeviceMetadata(DEV_FAMILY_CORSORI_3758L,
            Collections.emptyList(), List.of("CS137-AF/CS158-AF", "CS158-AF", "CS157-AF", "CS358-AF"));

    public static final List<VeSyncDeviceMetadata> SUPPORTED_MODEL_FAMILIES = Arrays.asList(COSORI3758L);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_AIR_FRYER);

    public VeSyncDeviceAirFryerHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected String[] getChannelsToRemove() {
        String[] toRemove = new String[] {};
        return toRemove;
    }

    @Override
    public void initialize() {
        super.initialize();
        customiseChannels();
    }

    @Override
    public void updateBridgeBasedPolls(final VeSyncBridgeConfiguration config) {
        Integer pollRate = config.airFryerPollInterval;
        if (pollRate == null) {
            pollRate = DEFAULT_AIR_FRYER_POLL_RATE;
        }
        if (ThingStatus.OFFLINE.equals(getThing().getStatus())) {
            setBackgroundPollInterval(-1);
        } else {
            setBackgroundPollInterval(pollRate);
        }
    }

    @Override
    public void dispose() {
        this.setBackgroundPollInterval(-1);
    }

    @Override
    public String getDeviceFamilyProtocolPrefix() {
        return DEV_TYPE_FAMILY_AIR_FRYER;
    }

    @Override
    public List<VeSyncDeviceMetadata> getSupportedDeviceMetadata() {
        return SUPPORTED_MODEL_FAMILIES;
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        final String deviceFamily = getThing().getProperties().get(DEVICE_PROP_DEVICE_FAMILY);
        if (deviceFamily == null) {
            return;
        }
    }

    @Override
    protected void pollForDeviceData(final ExpiringCache<String> cachedResponse) {
    }
}

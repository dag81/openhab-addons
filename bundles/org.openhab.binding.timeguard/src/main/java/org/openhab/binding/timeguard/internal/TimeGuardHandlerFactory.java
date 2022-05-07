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
package org.openhab.binding.timeguard.internal;

import static org.openhab.binding.timeguard.internal.TimeGuardBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.timeguard.internal.api.TimeGuardApiHelper;
import org.openhab.binding.timeguard.internal.handlers.TimeGuardBridgeHandler;
import org.openhab.binding.timeguard.internal.handlers.TimeGuardDeviceHandler;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link TimeGuardHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author david.goodyear@gmail.com - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.timeguard", service = ThingHandlerFactory.class)
public class TimeGuardHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_BRIDGE, THING_TYPE_DEVICE);

    private final TimeGuardApiHelper api = new TimeGuardApiHelper();

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        final ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            return new TimeGuardBridgeHandler((Bridge) thing, api);
        } else if (THING_TYPE_DEVICE.equals(thingTypeUID)) {
            return new TimeGuardDeviceHandler(thing);
        }

        return null;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Reference
    protected void setHttpClientFactory(HttpClientFactory httpClientFactory) {
        api.setHttpClient(httpClientFactory.getCommonHttpClient());
    }

    protected void unsetHttpClientFactory(HttpClientFactory httpClientFactory) {
        api.setHttpClient(null);
    }
}

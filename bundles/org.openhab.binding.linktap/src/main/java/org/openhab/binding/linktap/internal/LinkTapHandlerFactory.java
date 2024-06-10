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
package org.openhab.binding.linktap.internal;

import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.linktap.handlers.LinkTapBridgeHandler;
import org.openhab.binding.linktap.protocol.servers.BindingServlet;
import org.openhab.binding.linktap.protocol.servers.IHttpClientProvider;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LinkTapHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.linktap", service = ThingHandlerFactory.class)
public class LinkTapHandlerFactory extends BaseThingHandlerFactory implements IHttpClientProvider {

    private final Logger logger = LoggerFactory.getLogger(LinkTapHandlerFactory.class);

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_SAMPLE, THING_TYPE_BRIDGE);

    private final HttpService httpService;
    private final BindingServlet bindingServlet;

    @Activate
    public LinkTapHandlerFactory(@Reference HttpService httpService) {
        this.httpService = httpService;
        this.bindingServlet = new BindingServlet(httpService, logger);
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_SAMPLE.equals(thingTypeUID)) {
            return new LinkTapHandler(thing);
        } else if (THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            return new LinkTapBridgeHandler((Bridge) thing, this);
        }

        return null;
    }

    private @Nullable HttpClient httpClientRef = null;

    @Override
    public @Nullable HttpClient getHttpClient() {
        return httpClientRef;
    }

    @Reference
    protected void setHttpClientFactory(HttpClientFactory httpClientFactory) {
        httpClientRef = httpClientFactory.getCommonHttpClient();
    }
}

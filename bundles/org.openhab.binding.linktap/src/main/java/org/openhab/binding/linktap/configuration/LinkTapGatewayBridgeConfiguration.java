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
package org.openhab.binding.linktap.configuration;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link LinkTapGatewayBridgeConfiguration} class contains fields mapping the configuration parameters for the
 * bridge.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class LinkTapGatewayBridgeConfiguration {

    /**
     * The Gateways IP address / Hostname.
     */
    @Nullable
    public String hostname = "";
}

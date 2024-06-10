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
package org.openhab.binding.linktap.protocol.servers;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;

/**
 * The {@link BindingServlet} defines the request to enable or disable alerts from a given device.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class LinkTapApi {

    private @NonNullByDefault({}) HttpClient httpClient;

    /**
     * Sets the httpClient object to be used for API calls to Vesync.
     *
     * @param httpClient the client to be used.
     */
    public void setHttpClient(@Nullable HttpClient httpClient) {
        this.httpClient = httpClient;
    }
}

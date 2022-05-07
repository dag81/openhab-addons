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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.timeguard.internal.dto.requests.TimeGuardRequest;
import org.openhab.binding.timeguard.internal.exceptions.AuthenticationException;
import org.openhab.binding.timeguard.internal.exceptions.CommunicationsIssueException;
import org.openhab.binding.timeguard.internal.exceptions.DeviceUnknownException;

/**
 * The {@link TimeGuardClient} is TBC.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public interface TimeGuardClient {

    String reqAuthorized(final String url, final HttpMethod method, final @Nullable TimeGuardRequest requestData)
            throws AuthenticationException, DeviceUnknownException, CommunicationsIssueException;
}

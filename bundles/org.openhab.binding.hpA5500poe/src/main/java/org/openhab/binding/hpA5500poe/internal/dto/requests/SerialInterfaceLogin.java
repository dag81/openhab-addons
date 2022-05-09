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
package org.openhab.binding.hpA5500poe.internal.dto.requests;

import java.util.regex.Pattern;

/**
 * The {@link SerialInterfaceLogin} is the Java class as a DTO to hold login credentials for the Vesync
 * API.
 *
 * @author David Goodyear - Initial contribution
 */
public class SerialInterfaceLogin extends TelnetRequest {

    public SerialInterfaceLogin() {
        super();

        // This pattern must be matched before the command can be executed
        this.waitForMatchPattern = Pattern.compile(".*[P][a][s][s][w][o][r][d][:]$", Pattern.DOTALL);

        // If not null this pattern must be matched before the command can be considered as done
        this.completeMatchPattern = Pattern.compile(".*[<][0-9a-zA-Z_]+[>]$", Pattern.DOTALL);
        this.errorMatchPattern = Pattern.compile(".*Username or password is invalid.$", Pattern.DOTALL);
    }
}

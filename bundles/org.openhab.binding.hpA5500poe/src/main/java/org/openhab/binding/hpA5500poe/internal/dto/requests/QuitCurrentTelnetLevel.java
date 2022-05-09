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
 * The {@link QuitCurrentTelnetLevel} is the Java class as a DTO to hold login credentials for the Vesync
 * API.
 *
 * @author David Goodyear - Initial contribution
 */
public class QuitCurrentTelnetLevel extends TelnetRequest {

    public QuitCurrentTelnetLevel() {
        super();

        this.commandToSend = "quit";

        // If not null this pattern must be matched before the command can be considered as done
        this.completeMatchPattern = Pattern.compile("(.*[<][0-9a-zA-Z_]+[>]$)||(.*[\\[][0-9a-zA-Z_]+[\\]]$)",
                Pattern.DOTALL);
    }
}

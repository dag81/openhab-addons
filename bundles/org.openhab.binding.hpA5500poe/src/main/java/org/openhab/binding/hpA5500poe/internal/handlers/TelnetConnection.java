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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.openhab.binding.hpA5500poe.internal.dto.requests.TelnetRequest;
import org.openhab.binding.hpA5500poe.internal.dto.responses.TelnetResponse;

/**
 * The {@link TelnetConnection} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Goodyear - Initial contribution
 */
public class TelnetConnection {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private char[] readBuffer = new char[4096];
    private final StringBuilder strBldr = new StringBuilder();

    private void sanitiseBuffer(int lengthToProcess) {
        int curVal = 0;
        for (int i = 0; i < lengthToProcess; ++i) {
            curVal = (int) readBuffer[i];
            if (curVal == (int) '\r' || curVal == (int) '\n' || curVal == (int) '\t')
                continue;
            if (curVal < 32 || curVal > 126)
                readBuffer[i] = ' ';
        }
    }

    public TelnetConnection() {
        super();
    }

    public synchronized void connect(final String host) throws UnknownHostException, IOException {
        if (clientSocket != null) {
            disconnect();
        }
        clientSocket = new Socket(host, 23);
        out = new PrintWriter(clientSocket.getOutputStream(), false);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public synchronized TelnetResponse sendRequest(final TelnetRequest request) throws IOException {

        boolean preConditionMatched = request.waitForMatchPattern == null; // If null nothing to match - so considered
                                                                           // matched

        // Must wait for a matching pattern to be consumed prior to sending
        while (!preConditionMatched) {
            int charsRead = in.read(readBuffer, 0, readBuffer.length);
            if (charsRead > 0) {
                sanitiseBuffer(charsRead);
                strBldr.append(readBuffer, 0, charsRead);
            }
            if (request.waitForMatchPattern.matcher(strBldr.toString()).matches()) {
                preConditionMatched = true;
            }
        }

        // Send the command with a return character as would be sent via Telnet
        if (request.commandToSend != null) {
            // Flush the current stored data - as now we want any response data after the command is sent
            if (strBldr.length() > 0) {
                strBldr.delete(0, strBldr.length());
            }
            out.write(request.commandToSend);
            out.write("\r");
            out.flush();
        }

        // Read back the response
        boolean postConditionMatched = request.completeMatchPattern == null; // If null nothing to match - so considered
                                                                             // matched

        boolean goodResponse = true;
        while (!postConditionMatched) {
            int charsRead = in.read(readBuffer, 0, readBuffer.length);
            if (charsRead > 0) {
                sanitiseBuffer(charsRead);
                strBldr.append(readBuffer, 0, charsRead);
            }
            String strRef = strBldr.toString();
            if (request.completeMatchPattern != null && request.completeMatchPattern.matcher(strRef).matches()) {
                postConditionMatched = true;
            } else if (request.errorMatchPattern != null && request.errorMatchPattern.matcher(strRef).matches()) {
                postConditionMatched = true;
                goodResponse = false;
            }
        }

        // Generate the response information
        final TelnetResponse response = new TelnetResponse(request, strBldr.toString(), goodResponse);
        if (strBldr.length() > 0) {
            strBldr.delete(0, strBldr.length());
        }
        return response;
    }

    public synchronized void disconnect() {
        if (out != null) {
            out.close();
            out = null;
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException ioe) {
            } finally {
                in = null;
            }
        }

        if (clientSocket == null)
            return;
        try {
            clientSocket.close();
        } catch (IOException ioe) {
            try {
                clientSocket.close();
            } catch (IOException ioe2) {
            }
        }
    }
}

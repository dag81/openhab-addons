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
package org.openhab.binding.linktap.protocol;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link HandshakeResp} informs the Gateway of the current date, time and weekday in response to
 * a HandshakeReq Frame.
 *
 * @provides Gw: Expects response of HandshakeResp, to inform the Gateway of the current local Date and Time
 * @replyTo HandshakeReq
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class HandshakeResp extends TLGatewayFrame {

    public HandshakeResp() {
    }

    /**
     * Defines the date in the format YYYYMMDD
     */
    @SerializedName("date")
    protected String date = EMPTY_STRING;
    public String getDate() { return date; }
    public void setDate(final String date) {
        this.date = date;
    }
    /**
     * Defines the time for the GW in the format HHMMSS
     */
    @SerializedName("time")
    protected String time = EMPTY_STRING;
    public String getTime() {
        return time;
    }
    public void setTime(final String time) {
        this.time = time;
    }


    /**
     * Defines the weekday for the GW
     * 1 represents Monday.... 7 represents Sunday
     */
    @SerializedName("wday")
    protected int wday = DEFAULT_INT;
    public int getWday() { return wday; }
    public void setWday(final int weekdayNo) {
        this.wday = weekdayNo;
    }
    static final String CUSTOM_PATTERN = "yyyyMMdd";
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(CUSTOM_PATTERN);

    public boolean isValid() {
        if (!super.isValid())
            return false;

        if (wday < 1 || wday > 7)
            return false;

        try {
            LocalDate.parse(date, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return false;
        }

        // TODO: Validate Time Field

        return true;
    }
}

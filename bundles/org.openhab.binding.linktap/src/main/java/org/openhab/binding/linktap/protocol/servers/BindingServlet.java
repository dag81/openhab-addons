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

import static org.openhab.binding.linktap.protocol.frames.TLGatewayFrame.DEFAULT_INT;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.linktap.internal.LinkTapBindingConstants;
import org.openhab.binding.linktap.protocol.frames.TLGatewayFrame;
import org.openhab.binding.linktap.protocol.processors.TransactionProcessor;
import org.openhab.core.thing.Thing;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BindingServlet} defines the request to enable or disable alerts from a given device.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class BindingServlet extends HttpServlet {
    private static final long serialVersionUID = -23L;

    private Logger logger = LoggerFactory.getLogger(BindingServlet.class);

    String servletUrlWithoutRoot;
    String servletUrl;
    HttpService httpService;

    List<Thing> accountHandlers = new ArrayList<>();

    public BindingServlet(HttpService httpService, Logger logger) {
        this.httpService = httpService;
        logger.warn("Servlet host port: {}", this.httpService.createDefaultHttpContext().toString());
        this.logger = logger;
        servletUrlWithoutRoot = "linkTap";
        servletUrl = "/" + servletUrlWithoutRoot;
        try {
            httpService.registerServlet(servletUrl, this, null, httpService.createDefaultHttpContext());
        } catch (NamespaceException | ServletException e) {
            logger.warn("Register servlet fails", e);
        }
    }

    public void dispose() {
        httpService.unregister(servletUrl);
    }

    @Override
    protected void doGet(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp)
            throws ServletException, IOException {

        logger.warn("Got GET request from {}", req.getRemoteAddr());
        logger.warn("Got GET request from {}", req.getRemoteHost());

        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>" + "My first page" + "</title><head><body>");
        html.append("<h1>" + "Some Heading" + "</h1>");

        if (req == null) {
            return;
        }

        html.append("<body><b>Remote Host</b>").append(req.getRemoteHost()).append("</body>");
        html.append("<body><b>Remote Addr</b>").append(req.getRemoteAddr()).append("</body>");
        html.append("<body><b>Remote User</b>").append(req.getRemoteUser()).append("</body>");
        html.append("<body><b>Remote Port</b>").append(req.getRemotePort()).append("</body>");

        if (resp == null) {
            return;
        }

        String requestUri = req.getRequestURI();
        if (requestUri == null) {
            return;
        }
        String uri = requestUri.substring(servletUrl.length());
        String queryString = req.getQueryString();
        if (queryString != null && queryString.length() > 0) {
            uri += "?" + queryString;
        }
        logger.debug("doGet {}", uri);

        if (!"/".equals(uri)) {
            String newUri = req.getServletPath() + "/";
            resp.sendRedirect(newUri);
            return;
        }

        html.append("</body></html>");

        resp.addHeader("content-type", "text/html;charset=UTF-8");
        try {
            resp.getWriter().write(html.toString());
        } catch (IOException e) {
            logger.warn("return html failed with uri syntax error", e);
        }
    }

    @Override
    protected void doPost(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp)
            throws ServletException, IOException {
        logger.warn("Got request from {}", req.getRemoteAddr());
        logger.warn("Got request from {}", req.getRemoteHost());
        Enumeration<String> headers = req.getHeaderNames();
        for (Iterator<String> it = headers.asIterator(); it.hasNext();) {
            String header = it.next();
            logger.warn("Got header {} with value {}", header, req.getHeader(header));
        }

        int bufferSize = 1000; // The payload string is technically limited to 768 characters - this should be enough
                               // for one buffer for the whole lot
        char[] buffer = new char[bufferSize];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0;) {
            out.append(buffer, 0, numRead);
        }

        String payload = out.toString();
        logger.warn("Output {}", payload);
        final TLGatewayFrame tlFrame = LinkTapBindingConstants.GSON.fromJson(payload, TLGatewayFrame.class);
        if (tlFrame.command == DEFAULT_INT) {
            logger.warn("Unsolicited frame - Mapping to CMD 3");
        } else {
            logger.warn("Got Command {}", tlFrame.command);
            TransactionProcessor tp = TransactionProcessor.getInstance();
            String result = tp.ProcessGwRequest(req.getRemoteAddr(), tlFrame.command, payload);
            resp.setContentType("application/json");
            resp.setContentLength(result.length());
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().print(result);
            resp.flushBuffer();
        }
    }
}

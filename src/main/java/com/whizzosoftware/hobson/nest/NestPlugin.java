/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.nest;

import com.whizzosoftware.hobson.api.device.DeviceContext;
import com.whizzosoftware.hobson.api.device.DeviceNotFoundException;
import com.whizzosoftware.hobson.api.device.HobsonDevice;
import com.whizzosoftware.hobson.api.plugin.PluginStatus;
import com.whizzosoftware.hobson.api.plugin.http.AbstractHttpClientPlugin;
import com.whizzosoftware.hobson.api.plugin.http.HttpRequest;
import com.whizzosoftware.hobson.api.plugin.http.HttpResponse;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.nest.dto.*;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * The Hobson driver that creates and updated devices via the Nest API.
 *
 * @author Dan Noguerol
 */
public class NestPlugin extends AbstractHttpClientPlugin {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String USER_AGENT = "Nest/3.0.1.15 (iOS) os=6.0 platform=iPad3,1";

    private LoginContext nestContext;

    public NestPlugin(String pluginId) {
        super(pluginId);
    }

    public String getName() {
        return "Nest";
    }

    public void onStartup(PropertyContainer config) {
        onPluginConfigurationUpdate(config);
    }

    public void onShutdown() {}

    @Override
    public long getRefreshInterval() {
        return 300;
    }

    public void onPluginConfigurationUpdate(PropertyContainer config) {
        // get the username and password from configuration
        String username = (String)config.getPropertyValue("username");
        String password = (String)config.getPropertyValue("password");

        // if they've been set, initialize the API object
        if (username != null && password != null) {
            sendLoginRequest(username, password);
        } else {
            logger.debug("Nest username and password are not set");
            setStatus(PluginStatus.notConfigured("Nest username and password are not set"));
        }
    }

    @Override
    public void onRefresh() {
        if (nestContext != null) {
            logger.debug("Refreshing Nest status");
            sendStatusRequest();
        }
    }

    private void sendLoginRequest(String username, String password) {
        try {
            URI uri = new URI("https://home.nest.com/user/login");

            logger.debug("Sending login request using for user {} with URI: {}", username, uri);

            String entity = "username=" + URLEncoder.encode(username, "UTF8") + "&password=" + URLEncoder.encode(password, "UTF8");
            logger.trace("POST data: {}", entity);

            sendHttpRequest(
                uri,
                HttpRequest.Method.POST,
                null,
                null,
                entity.getBytes(),
                "login"
            );
        } catch (Exception e) {
            logger.error("Error sending login request", e);
        }
    }

    private void sendStatusRequest() {
        try {
            URI uri = new URI(nestContext.getTransportUrl() + "/v2/mobile/" + nestContext.getUser());

            logger.debug("Sending status request using URI: {}", uri);

            Map<String,String> headers = new HashMap<>();
            headers.put("Authorization", "Basic " + nestContext.getAccessToken());
            headers.put("Accept", "*/*");
            headers.put("Accept-Encoding", "gzip, deflate");
            headers.put("Accept-Language", "en-us");
            headers.put("X-nl-protocol-version", "1");
            headers.put("X-nl-user-id", nestContext.getUser());
            headers.put("user-agent", USER_AGENT);

            sendHttpRequest(
                uri,
                HttpRequest.Method.GET,
                headers,
                "status"
            );
        } catch (URISyntaxException e) {
            logger.error("Error sending status request", e);
        }
    }

    public void sendSetTargetTemperatureRequest(String deviceId, Double t) {
        try {
            URI uri = new URI(nestContext.getTransportUrl() + "/v2/put/shared." + deviceId);

            logger.debug("Setting target temperature using URI: {}", uri);

            Map<String,String> headers = new HashMap<>();
            headers.put("user-agent", USER_AGENT);
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", "Basic " + nestContext.getAccessToken());

            String entity = "{\"target_change_pending\":true,\"target_temperature\":" + t + "}";
            logger.trace("POST data: {}", entity);

            sendHttpRequest(
                uri,
                HttpRequest.Method.POST,
                headers,
                null,
                entity.getBytes(),
                "setTemp"
            );
        } catch (URISyntaxException e) {
            logger.error("Error sending setTemp request", e);
        }
    }

    private void processStatus(Status status) {
        if (status.getStructureCount() > 0) {
            if (status.getStructureCount() == 1) {
                Structure structure = (Structure) status.getStructures().toArray()[0];
                String[] devices = structure.getDevices();
                for (String deviceId : devices) {
                    if (deviceId.startsWith("device.")) {
                        deviceId = deviceId.substring(7);
                    }
                    Shared sharedDTO = status.getShared(deviceId);
                    if (sharedDTO != null) {
                        try {
                            try {
                                HobsonDevice device = getDevice(DeviceContext.create(getContext(), deviceId));
                                logger.debug("Updating state of device: " + deviceId);
                                if (device instanceof NestThermostat) {
                                    ((NestThermostat)device).updateStatus(sharedDTO);
                                } else {
                                    logger.error("Status update expected device {} to be a thermostat but was: {}", deviceId, device);
                                }
                            } catch (DeviceNotFoundException dnfe) {
                                logger.debug("Creating Nest device: " + deviceId);
                                NestThermostat nt = new NestThermostat(this, deviceId, sharedDTO, this);
                                publishDevice(nt);
                            }
                        } catch (Exception e) {
                            logger.error("Error updating device with ID: " + deviceId, e);
                        }
                    } else {
                        logger.error("Structure defines a device ID that doesn't have a shared record");
                    }
                }
            } else {
                logger.error("Only one Nest structure is supported by this plugin");
                setStatus(PluginStatus.failed("Only one structure is supported by this plugin"));
            }
        } else {
            logger.error("No Nest structure has been defined");
            setStatus(PluginStatus.failed("No Nest structure has been defined"));
        }
    }

    @Override
    protected TypedProperty[] createSupportedProperties() {
        return new TypedProperty[] {
            new TypedProperty.Builder("username", "User name", "Your Nest user name (same as web site)", TypedProperty.Type.STRING).build(),
            new TypedProperty.Builder("password", "Password", "Your Nest password (same as web site)", TypedProperty.Type.SECURE_STRING).build()
        };
    }

    @Override
    public void onHttpResponse(HttpResponse response, Object context) {
        try {
            if ("login".equals(context)) {
                String s = response.getBody();
                JSONObject json = new JSONObject(new JSONTokener(s));
                logger.debug("Login response received: {}", response.getStatusCode());
                logger.trace(s);
                nestContext = new LoginContext(json);
                logger.debug("Login context received: {}", nestContext);
                setStatus(PluginStatus.running());
                sendStatusRequest();
            } else if ("status".equals(context)) {
                String s = response.getBody();
                JSONObject json = new JSONObject(new JSONTokener(s));
                logger.debug("Status response received: {}", response.getStatusCode());
                logger.trace(s);
                Status status = new Status(json);
                processStatus(status);
            } else {
                logger.debug("Response {} received: {}", context, response.getStatusCode());
                logger.trace(response.getBody());
            }
        } catch (IOException e) {
            logger.error("Error processing HTTP response", e);
        }
    }

    @Override
    public void onHttpRequestFailure(Throwable cause, Object context) {
        logger.error("HTTP request failed", cause);
    }
}

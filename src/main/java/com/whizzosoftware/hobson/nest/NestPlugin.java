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
import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import com.whizzosoftware.hobson.api.plugin.PluginStatus;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.nest.api.NestApi;
import com.whizzosoftware.hobson.nest.api.NestLoginContext;
import com.whizzosoftware.hobson.nest.api.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The Hobson driver that creates and updated devices via the Nest API.
 *
 * @author Dan Noguerol
 */
public class NestPlugin extends AbstractHobsonPlugin {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private NestApi nestApi;
    private NestLoginContext nestContext;

    public NestPlugin(String pluginId) {
        super(pluginId);
    }

    public String getName() {
        return "Nest";
    }

    public NestApi getApi() {
        return nestApi;
    }

    public NestLoginContext getLoginContext() {
        return nestContext;
    }

    public void onStartup(PropertyContainer config) {
        onPluginConfigurationUpdate(config);
    }

    public void onShutdown() {

    }

    @Override
    public long getRefreshInterval() {
        return 300;
    }

    public void onPluginConfigurationUpdate(PropertyContainer config) {
        try {
            // get the username and password from configuration
            String username = (String)config.getPropertyValue("username");
            String password = (String)config.getPropertyValue("password");

            // if they've been set, initialize the API object
            if (username != null && password != null) {
                logger.debug("Initializing Nest API with user: {}", username);
                nestApi = new NestApi(username, password);
                logger.debug("Performing Nest login");
                nestContext = nestApi.login();
                logger.debug("Nest login successful; transport URL is {}", nestContext.getTransportUrl());

                setStatus(PluginStatus.running());

                onRefresh();
            } else {
                logger.debug("Nest username and password are not set");
                setStatus(PluginStatus.notConfigured("Nest username and password are not set"));
            }
        } catch (IOException e) {
            logger.error("An error occurred starting the Nest plugin", e);
            setStatus(PluginStatus.failed(e.getLocalizedMessage()));
            nestApi = null;
        }
    }

    @Override
    public void onRefresh() {
        if (nestApi != null) {
            logger.debug("Refreshing Nest status");

            try {
                Status status = nestApi.getStatus(nestContext);
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
            } catch (IOException e) {
                logger.error("Error reading Nest status information", e);
                setStatus(PluginStatus.failed(e.getLocalizedMessage()));
            }
        }
    }

    @Override
    protected TypedProperty[] createSupportedProperties() {
        return new TypedProperty[] {
            new TypedProperty.Builder("username", "User name", "Your Nest user name (same as web site)", TypedProperty.Type.STRING).build(),
            new TypedProperty.Builder("password", "Password", "Your Nest password (same as web site)", TypedProperty.Type.SECURE_STRING).build()
        };
    }
}

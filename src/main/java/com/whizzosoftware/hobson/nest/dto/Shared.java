/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.nest.dto;

import org.json.JSONObject;

public class Shared {
    private Double currentTemperature;
    private String name;
    private Double targetTemperature;
    private Double targetTemperatureHigh;
    private Double targetTemperatureLow;
    private String targetTemperatureType;
    private Boolean targetChangePending;

    public Shared(JSONObject json) {
        currentTemperature = json.getDouble("current_temperature");
        targetTemperature = json.getDouble("target_temperature");
        targetTemperatureHigh = json.getDouble("target_temperature_high");
        targetTemperatureLow = json.getDouble("target_temperature_low");
        targetTemperatureType = json.getString("target_temperature_type");
        targetChangePending = json.getBoolean("target_change_pending");

        String s = json.getString("name");
        name = (s != null && s.trim().length() > 0) ? s : null;
    }

    public Double getCurrentTemperature() {
        return currentTemperature;
    }

    public String getName() {
        return name;
    }

    public Double getTargetTemperature() {
        return targetTemperature;
    }

    public Double getTargetTemperatureHigh() {
        return targetTemperatureHigh;
    }

    public Double getTargetTemperatureLow() {
        return targetTemperatureLow;
    }

    public String getTargetTemperatureType() {
        return targetTemperatureType;
    }

    public Boolean getTargetChangePending() {
        return targetChangePending;
    }
}

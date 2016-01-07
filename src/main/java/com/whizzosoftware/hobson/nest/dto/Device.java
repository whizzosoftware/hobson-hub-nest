/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.nest.dto;

import org.json.JSONObject;

public class Device {
    private String currentVersion;
    private String fanMode;
    private Boolean hasAirFilter;
    private Boolean hasDehumidifier;
    private Boolean hasFan;
    private Boolean hasHeatPump;
    private Boolean hasHumidifier;
    private Integer targetHumidity;
    private Boolean leaf;
    private String temperatureScale;

    public Device(JSONObject json) {
        currentVersion = json.getString("current_version");
        fanMode = json.getString("fan_mode");
        hasAirFilter = json.getBoolean("has_air_filter");
        hasDehumidifier = json.getBoolean("has_dehumidifier");
        hasFan = json.getBoolean("has_fan");
        hasHeatPump = json.getBoolean("has_heat_pump");
        hasHumidifier = json.getBoolean("has_humidifier");
        targetHumidity = json.getInt("target_humidity");
        leaf = json.getBoolean("leaf");
        temperatureScale = json.getString("temperature_scale");
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getFanMode() {
        return fanMode;
    }

    public Boolean getHasAirFilter() {
        return hasAirFilter;
    }

    public Boolean getHasDehumidifier() {
        return hasDehumidifier;
    }

    public Boolean getHasFan() {
        return hasFan;
    }

    public Boolean getHasHeatPump() {
        return hasHeatPump;
    }

    public Boolean getHasHumidifier() {
        return hasHumidifier;
    }

    public Integer getTargetHumidity() {
        return targetHumidity;
    }

    public Boolean getLeaf() {
        return leaf;
    }

    public String getTemperatureScale() {
        return temperatureScale;
    }
}

/*
 *******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************
*/
package com.whizzosoftware.hobson.nest;

import com.whizzosoftware.hobson.api.device.DeviceType;
import com.whizzosoftware.hobson.api.device.proxy.AbstractHobsonDeviceProxy;
import com.whizzosoftware.hobson.api.plugin.HobsonPlugin;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.variable.VariableConstants;
import com.whizzosoftware.hobson.api.variable.VariableMask;
import com.whizzosoftware.hobson.nest.dto.Shared;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * A Hobson device representing a Nest thermostat.
 *
 * @author Dan Noguerol
 */
public class NestThermostat extends AbstractHobsonDeviceProxy {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private NestPlugin nestPlugin;
    private Shared initialData;

    NestThermostat(HobsonPlugin plugin, String id, Shared initialData, NestPlugin nestPlugin) {
        super(plugin, id, initialData.getName() != null ? initialData.getName() : "Nest", DeviceType.THERMOSTAT);
        this.initialData = initialData;
        this.nestPlugin = nestPlugin;
    }

    @Override
    public void onStartup(String name, Map<String,Object> config) {
        Double currentTempC = initialData.getCurrentTemperature();
        Double targetTempC = initialData.getTargetTemperature();

        long now = System.currentTimeMillis();
        setLastCheckin(now);

        publishVariables(
            createDeviceVariable(VariableConstants.INDOOR_TEMP_C, VariableMask.READ_ONLY, currentTempC, now),
            createDeviceVariable(VariableConstants.INDOOR_TEMP_F, VariableMask.READ_ONLY, convertCelsiusToFahrenheit(currentTempC), now),
            createDeviceVariable(VariableConstants.TARGET_TEMP_C, VariableMask.READ_WRITE, targetTempC, now),
            createDeviceVariable(VariableConstants.TARGET_TEMP_F, VariableMask.READ_WRITE, convertCelsiusToFahrenheit(targetTempC), now)
        );
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public String getManufacturerName() {
        return "Nest";
    }

    @Override
    public String getManufacturerVersion() {
        return null;
    }

    @Override
    public String getModelName() {
        return null;
    }

    @Override
    public String getPreferredVariableName() {
        return VariableConstants.INDOOR_TEMP_F;
    }

    @Override
    public void onDeviceConfigurationUpdate(Map<String,Object> config) {
    }

    @Override
    protected TypedProperty[] getConfigurationPropertyTypes() {
        return null;
    }

    public void onSetVariables(Map<String,Object> values) {
        for (String name : values.keySet()) {
            Object value = values.get(name);
            if (VariableConstants.TARGET_TEMP_C.equals(name)) {
                Double f = getEventAsDouble(value);
                if (f != null) {
                    // send the update to Nest
                    nestPlugin.sendSetTargetTemperatureRequest(getContext().getDeviceId(), f);
                    // TODO: we should probably do a status update instead
                    setVariableValue(name, value, System.currentTimeMillis());
                } else {
                    logger.error("Attempt to set temperature with no float value: {}", value);
                }
            } else if (VariableConstants.TARGET_TEMP_F.equals(name)) {
                Double f = getEventAsDouble(value);
                if (f != null) {
                    // send the update to Nest
                    nestPlugin.sendSetTargetTemperatureRequest(getContext().getDeviceId(), convertFahrenheitToCelsius(f));
                    // TODO: we should probably do a status update instead
                    setVariableValue(name, value, System.currentTimeMillis());
                } else {
                    logger.error("Attempt to set temperature with no float value: {}", value);
                }
            }
        }
    }

    /**
     * Called when a new Status is received from the Nest API.
     *
     * @param shared the Shared DTO to use for the update
     */
    void updateStatus(Shared shared) {
        Double currentTempC = shared.getCurrentTemperature();
        Double targetTempC = shared.getTargetTemperature();

        Map<String,Object> values = new HashMap<>();
        values.put(VariableConstants.INDOOR_TEMP_C, currentTempC);
        values.put(VariableConstants.INDOOR_TEMP_F, convertCelsiusToFahrenheit(currentTempC));
        values.put(VariableConstants.TARGET_TEMP_C, targetTempC);
        values.put(VariableConstants.TARGET_TEMP_F, convertCelsiusToFahrenheit(targetTempC));

        setVariableValues(values);
        setLastCheckin(System.currentTimeMillis());
    }

    /**
     * Convenience method to convert Celsius to Fahrenheit.
     *
     * @param celsius the temperature in Celsius
     *
     * @return the temperature in Fahrenheit
     */
    private Double convertCelsiusToFahrenheit(Double celsius) {
        return celsius * 9 / 5 + 32;
    }

    /**
     * Convenience method to convert Fahrenheit to Celsius.
     *
     * @param fahrenheit the temperature in Fahrenheit
     *
     * @return the temperature in Celsius
     */
    private Double convertFahrenheitToCelsius(Double fahrenheit) {
        return (fahrenheit - 32) * 5 / 9;
    }

    /**
     * Returns a String or Float Object as a Float.
     *
     * @param o the object
     *
     * @return a Float (or null if it can't be converted)
     */
    private Double getEventAsDouble(Object o) {
        if (o instanceof String) {
            try {
                return Double.parseDouble((String)o);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (o instanceof Double) {
            return (Double)o;
        } else {
            return null;
        }
    }
}

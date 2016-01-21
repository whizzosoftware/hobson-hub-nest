/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.nest;

import com.whizzosoftware.hobson.api.device.AbstractHobsonDevice;
import com.whizzosoftware.hobson.api.device.DeviceType;
import com.whizzosoftware.hobson.api.plugin.HobsonPlugin;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import com.whizzosoftware.hobson.api.variable.HobsonVariable;
import com.whizzosoftware.hobson.api.variable.VariableConstants;
import com.whizzosoftware.hobson.api.variable.VariableContext;
import com.whizzosoftware.hobson.api.variable.VariableUpdate;
import com.whizzosoftware.hobson.nest.dto.Shared;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A Hobson device representing a Nest thermostat.
 *
 * @author Dan Noguerol
 */
public class NestThermostat extends AbstractHobsonDevice {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private NestPlugin nestPlugin;
    private Shared initialData;

    public NestThermostat(HobsonPlugin plugin, String id, Shared initialData, NestPlugin nestPlugin) {
        super(plugin, id);
        setDefaultName(initialData.getName() != null ? initialData.getName() : "Nest");
        this.initialData = initialData;
        this.nestPlugin = nestPlugin;
    }

    @Override
    public void onStartup(PropertyContainer config) {
        super.onStartup(config);

        Double currentTempC = initialData.getCurrentTemperature();
        Double targetTempC = initialData.getTargetTemperature();

        long now = System.currentTimeMillis();
        publishVariable(VariableConstants.INDOOR_TEMP_C, currentTempC, HobsonVariable.Mask.READ_ONLY, now);
        publishVariable(VariableConstants.INDOOR_TEMP_F, convertCelsiusToFahrenheit(currentTempC), HobsonVariable.Mask.READ_ONLY, now);
        publishVariable(VariableConstants.TARGET_TEMP_C, targetTempC, HobsonVariable.Mask.READ_WRITE, now);
        publishVariable(VariableConstants.TARGET_TEMP_F, convertCelsiusToFahrenheit(targetTempC), HobsonVariable.Mask.READ_WRITE, now);
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public String getPreferredVariableName() {
        return VariableConstants.INDOOR_TEMP_F;
    }

    @Override
    protected TypedProperty[] createSupportedProperties() {
        return null;
    }

    public DeviceType getType() {
        return DeviceType.THERMOSTAT;
    }

    public void onSetVariable(String name, Object value) {
        if (VariableConstants.TARGET_TEMP_C.equals(name)) {
            Double f = getEventAsDouble(value);
            if (f != null) {
                // send the update to Nest
                nestPlugin.sendSetTargetTemperatureRequest(getContext().getDeviceId(), f);
                // TODO: we should probably do a status update instead
                fireVariableUpdateNotification(name, value);
            } else {
                logger.error("Attempt to set temperature with no float value: {}", value);
            }
        } else if (VariableConstants.TARGET_TEMP_F.equals(name)) {
            Double f = getEventAsDouble(value);
            if (f != null) {
                // send the update to Nest
                nestPlugin.sendSetTargetTemperatureRequest(getContext().getDeviceId(), convertFahrenheitToCelsius(f));
                // TODO: we should probably do a status update instead
                fireVariableUpdateNotification(name, value);
            } else {
                logger.error("Attempt to set temperature with no float value: {}", value);
            }
        }
    }

    /**
     * Called when a new Status is received from the Nest API.
     *
     * @param shared the Shared DTO to use for the update
     */
    public void updateStatus(Shared shared) {
        Double currentTempC = shared.getCurrentTemperature();
        Double targetTempC = shared.getTargetTemperature();

        List<VariableUpdate> updates = new ArrayList<VariableUpdate>();
        updates.add(new VariableUpdate(VariableContext.create(getContext(), VariableConstants.INDOOR_TEMP_C), currentTempC));
        updates.add(new VariableUpdate(VariableContext.create(getContext(), VariableConstants.INDOOR_TEMP_F), convertCelsiusToFahrenheit(currentTempC)));
        updates.add(new VariableUpdate(VariableContext.create(getContext(), VariableConstants.TARGET_TEMP_C), targetTempC));
        updates.add(new VariableUpdate(VariableContext.create(getContext(), VariableConstants.TARGET_TEMP_F), convertCelsiusToFahrenheit(targetTempC)));
        fireVariableUpdateNotifications(updates);

        setDeviceAvailability(true, System.currentTimeMillis());
    }

    /**
     * Convenience method to convert Celsius to Fahrenheit.
     *
     * @param celsius the temperature in Celsius
     *
     * @return the temperature in Fahrenheit
     */
    protected Double convertCelsiusToFahrenheit(Double celsius) {
        return celsius * 9 / 5 + 32;
    }

    /**
     * Convenience method to convert Fahrenheit to Celsius.
     *
     * @param fahrenheit the temperature in Fahrenheit
     *
     * @return the temperature in Celsius
     */
    protected Double convertFahrenheitToCelsius(Double fahrenheit) {
        return (fahrenheit - 32) * 5 / 9;
    }

    /**
     * Returns a String or Float Object as a Float.
     *
     * @param o the object
     *
     * @return a Float (or null if it can't be converted)
     */
    protected Double getEventAsDouble(Object o) {
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

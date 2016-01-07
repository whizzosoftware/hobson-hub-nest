/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.nest.dto;

import org.json.JSONArray;
import org.json.JSONObject;

public class Structure {
    private Boolean away;
    public String location;
    public String postalCode;
    public String user;
    private String[] devices;
    public String[] swarm;

    public Structure(JSONObject json) {
        away = json.getBoolean("away");
        location = json.getString("location");
        postalCode = json.getString("postal_code");
        user = json.getString("user");

        JSONArray ja = json.getJSONArray("devices");
        devices = new String[ja.length()];
        for (int i=0; i < ja.length(); i++) {
            devices[i] = ja.getString(i);
        }

        ja = json.getJSONArray("swarm");
        swarm = new String[ja.length()];
        for (int i=0; i < ja.length(); i++) {
            swarm[i] = ja.getString(i);
        }
    }

    public Boolean getAway() {
        return away;
    }

    public String getLocation() {
        return location;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getUser() {
        return user;
    }

    public String[] getDevices() {
        return devices;
    }

    public String[] getSwarm() {
        return swarm;
    }
}

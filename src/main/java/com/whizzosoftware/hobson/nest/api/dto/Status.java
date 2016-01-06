/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.nest.api.dto;

import org.json.JSONObject;

import java.util.*;

public class Status {
    private Map<String,Structure> structures = new HashMap<String,Structure>();
    private Map<String,Device> devices = new HashMap<String,Device>();
    private Map<String,Shared> shared = new HashMap<String,Shared>();

    public Status(JSONObject json) {
        JSONObject c = json.getJSONObject("structure");
        for (Object o : c.keySet()) {
            String structId = (String)o;
            structures.put(structId, new Structure(c.getJSONObject(structId)));
        }

        c = json.getJSONObject("device");
        for (Object o : c.keySet()) {
            String deviceId = (String)o;
            devices.put(deviceId, new Device(c.getJSONObject(deviceId)));
        }

        c = json.getJSONObject("shared");
        for (Object o : c.keySet()) {
            String sharedId = (String)o;
            shared.put(sharedId, new Shared(c.getJSONObject(sharedId)));
        }
    }

    public Collection<Structure> getStructures() {
        return structures.values();
    }

    public int getStructureCount() {
        return structures.size();
    }

    public Structure getStructure(String id) {
        return structures.get(id);
    }

    public Device getDevice(String id) {
        return devices.get(id);
    }

    public int getSharedCount() {
        return shared.size();
    }

    public Shared getShared(String id) {
        return shared.get(id);
    }
}

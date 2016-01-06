/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.nest.api;

/**
 * Class that encapsulates information about a specific login.
 *
 * @author Dan Noguerol
 */
public class NestLoginContext {
    private String user;
    private String transportUrl;
    private String accessToken;

    public NestLoginContext(String user, String transportUrl, String accessToken) {
        this.user = user;
        this.transportUrl = transportUrl;
        this.accessToken = accessToken;
    }

    /**
     * Returns the logged-in user name.
     *
     * @return a String
     */
    public String getUser() {
        return user;
    }

    /**
     * Returns the transport URL that should be used as the URL prefix for all API calls.
     *
     * @return a String
     */
    public String getTransportUrl() {
        return transportUrl;
    }

    /**
     * Returns the access token that should be passed as a header to all API calls.
     *
     * @return a String
     */
    public String getAccessToken() {
        return accessToken;
    }
}

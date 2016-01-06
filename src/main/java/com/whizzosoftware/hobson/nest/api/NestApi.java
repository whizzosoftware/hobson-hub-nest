/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.nest.api;

import com.whizzosoftware.hobson.nest.api.dto.Status;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.zip.GZIPInputStream;

/**
 * A class that implements Nest's "unofficial" HTTP API.
 *
 * @author Dan Noguerol
 */
public class NestApi {
    public static final Logger logger = LoggerFactory.getLogger(NestApi.class);

    private static final String USER_AGENT = "Nest/3.0.1.15 (iOS) os=6.0 platform=iPad3,1";

    private String username;
    private String password;

    /**
     * Constructor.
     *
     * @param username the Nest user name
     * @param password the Nest user password
     */
    public NestApi(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Perform a user login.
     *
     * @return a NestLoginContext instance
     *
     * @throws IOException on failure
     */
    public NestLoginContext login() throws IOException {
        URL url = new URL("https://home.nest.com/user/login");

        logger.debug("Logging in using URL {}", url);

        HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("user-agent", USER_AGENT);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setUseCaches(false);

        String entity = "username=" + URLEncoder.encode(username, "UTF8") + "&password=" + URLEncoder.encode(password, "UTF8");
        logger.trace("POSTing data: {}", entity);

        OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
        osw.write(entity);
        osw.flush();
        osw.close();

        if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            logger.debug("Received successful login response");

            JSONObject json = new JSONObject(new JSONTokener(conn.getInputStream()));

            if (logger.isTraceEnabled()) {
                logger.trace("JSON response: {}", json);
            }

            return new NestLoginContext(
                json.getString("user"),
                json.getJSONObject("urls").getString("transport_url"),
                json.getString("access_token")
            );
        } else {
            logger.error("Received unexpected login response: {}", conn.getResponseCode());
            throw new IOException("Received unexpected HTTP response code: " + conn.getResponseCode());
        }
    }

    /**
     * Retrieves the Nest system status.
     *
     * @param context the login context
     *
     * @return a Status instance
     *
     * @throws IOException on failure
     */
    public Status getStatus(NestLoginContext context) throws IOException {
        URL url = new URL(context.getTransportUrl() + "/v2/mobile/" + context.getUser());

        logger.debug("Getting status from URL {}", url);

        HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setDoOutput(false);
        conn.setRequestProperty("Authorization", "Basic " + context.getAccessToken());
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        conn.setRequestProperty("Accept-Language", "en-us");
        conn.setRequestProperty("X-nl-protocol-version", "1");
        conn.setRequestProperty("X-nl-user-id", context.getUser());
        conn.setRequestProperty("user-agent", USER_AGENT);

        if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            logger.debug("Received successful status response");
            JSONObject json = new JSONObject(new JSONTokener(getInputStream(conn)));
            if (logger.isTraceEnabled()) {
                logger.trace("JSON response: {}", json);
            }
            return new Status(json);
        } else {
            logger.error("Received unexpected status response: {}", conn.getResponseCode());
            throw new IOException("Received unexpected HTTP response code: " + conn.getResponseCode());
        }
    }

    /**
     * Sets a Nest device's target temperature.
     *
     * @param context the login context
     * @param deviceId the serial number of the device to change
     * @param temperature the target temperature
     *
     * @throws IOException on failure
     */
    public void setTargetTemperature(NestLoginContext context, String deviceId, Double temperature) throws IOException {
        URL url = new URL(context.getTransportUrl() + "/v2/put/shared." + deviceId);

        logger.debug("Setting target temperature using URL {}", url);

        HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("user-agent", USER_AGENT);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Basic " + context.getAccessToken());
        conn.setUseCaches(false);

        OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
        String entity = "{\"target_change_pending\":true,\"target_temperature\":" + temperature + "}";
        logger.trace("POSTing data: {}", entity);
        osw.write(entity);
        osw.flush();
        osw.close();

        if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            logger.debug("Received successful response");
            conn.getInputStream().close();
        } else {
            logger.error("Received unexpected set temperature response: {}", conn.getResponseCode());
            throw new IOException("Received unexpected HTTP response code: " + conn.getResponseCode());
        }
    }

    /**
     * Generates an InputStream from an HttpsURLConnection. This will detect if the response is Gzip compressed
     * and set up the input stream chain appropriately.
     *
     * @param conn the connection object
     *
     * @return an InputStream
     *
     * @throws IOException on failure
     */
    protected InputStream getInputStream(HttpsURLConnection conn) throws IOException {
        if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) {
            return new GZIPInputStream(conn.getInputStream());
        } else {
            return conn.getInputStream();
        }
    }
}

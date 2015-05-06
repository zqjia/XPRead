
package com.xpread.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.xpread.service.HTTPSession.CookieHandler;
import com.xpread.service.HTTPResponse.ResponseException;

/**
 * Handles one session, i.e. parses the HTTP request and returns the response.
 */
public interface IHTTPSession {
    void execute() throws IOException, java.lang.Exception;

    Map<String, String> getParms();

    Map<String, String> getHeaders();

    /**
     * @return the path part of the URL.
     */
    String getUri();

    String getQueryParameterString();

    HTTPMethod getMethod();

    InputStream getInputStream();

    CookieHandler getCookies();

    /**
     * Adds the files in the request body to the files map.
     * 
     * @throws Exception
     * @throws java.lang.Exception
     * @arg files - map to modify
     */
    void parseBody(Map<String, String> files) throws IOException, ResponseException,
            java.lang.Exception;
}

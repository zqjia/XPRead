
package com.xpread.service;

/**
 * HTTP Request methods, with the ability to decode a <code>String</code> back
 * to its enum value.
 */
public enum HTTPMethod {
    GET, PUT, POST, DELETE, HEAD, OPTIONS;

    static HTTPMethod lookup(String method) {
        for (HTTPMethod m : HTTPMethod.values()) {
            if (m.toString().equalsIgnoreCase(method)) {
                return m;
            }
        }
        return null;
    }
}

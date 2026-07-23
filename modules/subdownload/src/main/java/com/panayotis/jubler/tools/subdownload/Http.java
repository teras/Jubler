/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal blocking HTTP helper built on {@link HttpURLConnection}: explicit timeouts, a hard response
 * size cap, and an optional handle so a superseded request can be disconnected. Runs off the EDT only.
 */
final class Http {

    static final String USER_AGENT = "Jubler v7.0";

    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;
    private static final int MAX_RESPONSE = 8 * 1024 * 1024; // 8 MiB cap for any single response

    private Http() {
    }

    static final class Response {
        final int code;
        final byte[] body;
        final String contentType;

        Response(int code, byte[] body, String contentType) {
            this.code = code;
            this.body = body;
            this.contentType = contentType;
        }

        String text() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    static Response get(String url, Map<String, String> headers, AtomicReference<HttpURLConnection> sink)
            throws IOException {
        return request("GET", url, headers, null, sink);
    }

    static Response post(String url, Map<String, String> headers, String body, AtomicReference<HttpURLConnection> sink)
            throws IOException {
        return request("POST", url, headers, body == null ? null : body.getBytes(StandardCharsets.UTF_8), sink);
    }

    private static Response request(String method, String url, Map<String, String> headers, byte[] body,
                                    AtomicReference<HttpURLConnection> sink) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        if (sink != null)
            sink.set(conn);
        try {
            conn.setRequestMethod(method);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setInstanceFollowRedirects(true);
            if (headers != null)
                for (Map.Entry<String, String> e : headers.entrySet())
                    conn.setRequestProperty(e.getKey(), e.getValue());
            if (body != null) {
                conn.setDoOutput(true);
                conn.getOutputStream().write(body);
                conn.getOutputStream().flush();
            }
            int code = conn.getResponseCode();
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            return new Response(code, readCapped(in), conn.getContentType());
        } finally {
            conn.disconnect();
        }
    }

    private static byte[] readCapped(InputStream in) throws IOException {
        if (in == null)
            return new byte[0];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0, n;
        while ((n = in.read(buf)) >= 0) {
            total += n;
            if (total > MAX_RESPONSE)
                throw new IOException("Response exceeds size limit");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}

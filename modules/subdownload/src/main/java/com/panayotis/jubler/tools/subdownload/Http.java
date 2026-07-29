/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal blocking HTTP helper built on {@link HttpURLConnection}: explicit timeouts, a hard response
 * size cap, and an optional handle so a superseded request can be disconnected. Runs off the EDT only.
 */
public final class Http {

    public static final String USER_AGENT = "Jubler v7.0";
    /** A real-browser User-Agent, required by Cloudflare-fronted sites that reject the plain Jubler agent. */
    public static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0 Safari/537.36";

    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;
    private static final int MAX_RESPONSE = 8 * 1024 * 1024; // 8 MiB cap for any single response

    /**
     * Modern TLS signature schemes (no legacy SHA-1) pinned on every HTTPS connection. Some bundled
     * runtimes (notably Temurin) still advertise {@code ecdsa_sha1/rsa_pkcs1_sha1/dsa_sha1}, which shifts
     * the TLS handshake fingerprint enough for Cloudflare bot filters to answer 403 (seen on
     * subs4series.com; plain API providers are unaffected). Pinning them per request keeps the fingerprint
     * consistent regardless of the runtime, applied exactly where the network call is made.
     */
    private static final String[] TLS_SIGNATURE_SCHEMES = {
            "ecdsa_secp256r1_sha256", "ecdsa_secp384r1_sha384", "ecdsa_secp521r1_sha512", "ed25519", "ed448",
            "rsa_pss_rsae_sha256", "rsa_pss_rsae_sha384", "rsa_pss_rsae_sha512",
            "rsa_pss_pss_sha256", "rsa_pss_pss_sha384", "rsa_pss_pss_sha512",
            "rsa_pkcs1_sha256", "rsa_pkcs1_sha384", "rsa_pkcs1_sha512"};

    /** {@code SSLParameters.setSignatureSchemes(String[])} exists since Java 19; null on older runtimes. */
    private static final Method SET_SIGNATURE_SCHEMES = resolveSetSignatureSchemes();
    private static final SSLSocketFactory HARDENED_SSL = SET_SIGNATURE_SCHEMES == null ? null
            : new HardenedSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());

    private Http() {
    }

    private static Method resolveSetSignatureSchemes() {
        try {
            return SSLParameters.class.getMethod("setSignatureSchemes", String[].class);
        } catch (NoSuchMethodException e) {
            return null; // pre-Java-19 runtime: keep the JVM's default schemes
        }
    }

    public static final class Response {
        public final int code;
        public final byte[] body;
        public final String contentType;
        final Map<String, List<String>> headers;

        Response(int code, byte[] body, String contentType, Map<String, List<String>> headers) {
            this.code = code;
            this.body = body;
            this.contentType = contentType;
            this.headers = headers;
        }

        public String text() {
            return new String(body, StandardCharsets.UTF_8);
        }

        public String text(Charset cs) {
            return new String(body, cs);
        }

        /** First value of the named response header, matched case-insensitively; null when absent. */
        public String header(String name) {
            if (headers == null || name == null)
                return null;
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                    List<String> values = e.getValue();
                    return values == null || values.isEmpty() ? null : values.get(0);
                }
            }
            return null;
        }

        /** All values of the named response header (e.g. multiple Set-Cookie), matched case-insensitively. */
        public List<String> headerValues(String name) {
            if (headers == null || name == null)
                return java.util.Collections.emptyList();
            for (Map.Entry<String, List<String>> e : headers.entrySet())
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name))
                    return e.getValue() == null ? java.util.Collections.<String>emptyList() : e.getValue();
            return java.util.Collections.emptyList();
        }
    }

    public static Response get(String url, Map<String, String> headers, AtomicReference<HttpURLConnection> sink)
            throws IOException {
        return request("GET", url, headers, null, true, sink);
    }

    public static Response post(String url, Map<String, String> headers, String body, AtomicReference<HttpURLConnection> sink)
            throws IOException {
        return request("POST", url, headers, body == null ? null : body.getBytes(StandardCharsets.UTF_8), true, sink);
    }

    /** GET that does not auto-follow redirects, so the caller can read the 3xx Location itself. */
    public static Response getNoFollow(String url, Map<String, String> headers, AtomicReference<HttpURLConnection> sink)
            throws IOException {
        return request("GET", url, headers, null, false, sink);
    }

    /** POST that does not auto-follow redirects, so the caller can read the 3xx Location itself. */
    public static Response postNoFollow(String url, Map<String, String> headers, String body,
                                 AtomicReference<HttpURLConnection> sink) throws IOException {
        return request("POST", url, headers, body == null ? null : body.getBytes(StandardCharsets.UTF_8), false, sink);
    }

    private static Response request(String method, String url, Map<String, String> headers, byte[] body,
                                    boolean followRedirects, AtomicReference<HttpURLConnection> sink) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        if (conn instanceof HttpsURLConnection && HARDENED_SSL != null)
            ((HttpsURLConnection) conn).setSSLSocketFactory(HARDENED_SSL);
        if (sink != null)
            sink.set(conn);
        try {
            conn.setRequestMethod(method);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setInstanceFollowRedirects(followRedirects);
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
            return new Response(code, readCapped(in), conn.getContentType(), conn.getHeaderFields());
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Wraps the default TLS factory and pins {@link #TLS_SIGNATURE_SCHEMES} on each socket it creates, so
     * the ClientHello never carries the legacy SHA-1 schemes regardless of the runtime's defaults. The
     * pinning uses the Java 19+ {@code SSLParameters.setSignatureSchemes} reflectively (this module is
     * compiled for Java 8) and silently leaves the defaults on older runtimes.
     */
    private static final class HardenedSSLSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;

        HardenedSSLSocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        private Socket harden(Socket socket) {
            if (socket instanceof SSLSocket && SET_SIGNATURE_SCHEMES != null) {
                SSLSocket ssl = (SSLSocket) socket;
                try {
                    SSLParameters params = ssl.getSSLParameters();
                    SET_SIGNATURE_SCHEMES.invoke(params, (Object) TLS_SIGNATURE_SCHEMES);
                    ssl.setSSLParameters(params);
                } catch (Exception ignored) {
                    // Leave the runtime's default schemes if pinning is unavailable for any reason.
                }
            }
            return socket;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket() throws IOException {
            return harden(delegate.createSocket());
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return harden(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return harden(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return harden(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return harden(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return harden(delegate.createSocket(address, port, localAddress, localPort));
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

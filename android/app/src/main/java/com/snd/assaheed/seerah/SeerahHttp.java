package com.snd.assaheed.seerah;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Small bounded HTTPS helper; no third-party HTTP stack. */
public final class SeerahHttp {
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final int BUFFER = 8192;

    private SeerahHttp() {
    }

    public static String get(String address) throws IOException {
        URL url = new URL(address);
        if (!"https".equals(url.getProtocol())) {
            throw new IOException("Only HTTPS is allowed");
        }
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "AsSaheeh-Seerah/1.0");
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + status);
            }
            int announced = connection.getContentLength();
            if (announced > MAX_BYTES) {
                throw new IOException("Response is too large");
            }
            input = connection.getInputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER];
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (output.size() + read > MAX_BYTES) {
                    throw new IOException("Response is too large");
                }
                output.write(buffer, 0, read);
            }
            return output.toString("UTF-8");
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String getWithFallback(String primary, String fallback) throws IOException {
        try {
            return get(primary);
        } catch (IOException first) {
            return get(fallback);
        }
    }
}

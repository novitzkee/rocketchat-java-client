package org.novitzkee.rocketchatclient.realtime;

import com.google.common.io.Resources;
import lombok.SneakyThrows;

import java.net.URL;
import java.nio.charset.StandardCharsets;

class RocketChatRealtimeMessages {

    static final String CONNECT_MESSAGE = loadMessageFile("json/realtime/connect.json");

    static final String CONNECTED_MESSAGE = loadMessageFile("json/realtime/connected.json");

    static final String PING_MESSAGE = loadMessageFile("json/realtime/ping.json");

    static final String PONG_MESSAGE = loadMessageFile("json/realtime/pong.json");

    static final String LOGIN_OK_RESPONSE = loadMessageFile("json/realtime/login_ok.json");

    static final String LOGIN_ERROR_RESPONSE = loadMessageFile("json/realtime/login_error.json");

    @SneakyThrows
    private static String loadMessageFile(String filename) {
        final URL url = Resources.getResource(filename);
        return Resources.toString(url, StandardCharsets.UTF_8);
    }
}

package org.novitzkee.rocketchatclient.realtime;

import com.google.common.io.Resources;
import lombok.SneakyThrows;

import java.net.URL;
import java.nio.charset.StandardCharsets;

class RocketChatRealtimeTestResponses {

    static final String OK_CONNECT_MESSAGE = loadMessageFile("json/realtime/connect_ok.json");

    static final String OK_LOGIN_MESSAGE = loadMessageFile("json/realtime/login_ok.json");

    static final String ERROR_LOGIN_MESSAGE = loadMessageFile("json/realtime/login_error.json");

    static final String PING_MESSAGE = loadMessageFile("json/realtime/ping.json");

    @SneakyThrows
    private static String loadMessageFile(String filename) {
        final URL url = Resources.getResource(filename);
        return Resources.toString(url, StandardCharsets.UTF_8);
    }
}

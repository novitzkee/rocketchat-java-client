package org.novitzkee.rocketchatclient.realtime;

import com.google.common.io.Resources;
import lombok.SneakyThrows;

import java.net.URL;
import java.nio.charset.StandardCharsets;

class RocketChatRealtimeMessages {

    private static final String CALL_ID_TEST_PLACEHOLDER = "{callId}";

    static final String CONNECT_MESSAGE = loadMessageFile("json/realtime/connect.json");

    static final String CONNECTED_MESSAGE = loadMessageFile("json/realtime/connected.json");

    static final String PING_MESSAGE = loadMessageFile("json/realtime/ping.json");

    static final String PONG_MESSAGE = loadMessageFile("json/realtime/pong.json");

    static String loginOkResponse(String callId) {
        return prepareMethodResponse("json/realtime/login_ok.json", callId);
    }

    static String loginErrorResponse(String callId) {
        return prepareMethodResponse("json/realtime/login_error.json", callId);
    }

    private static String prepareMethodResponse(String filename, String callId) {
        final String response = loadMessageFile(filename);
        return response.replace(CALL_ID_TEST_PLACEHOLDER, callId);
    }

    @SneakyThrows
    private static String loadMessageFile(String filename) {
        final URL url = Resources.getResource(filename);
        return Resources.toString(url, StandardCharsets.UTF_8);
    }
}

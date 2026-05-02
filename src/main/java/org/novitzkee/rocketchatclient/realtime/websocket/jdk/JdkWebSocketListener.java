package org.novitzkee.rocketchatclient.realtime.websocket.jdk;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.novitzkee.rocketchatclient.realtime.websocket.iface.RocketChatWebSocketListener;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
public class JdkWebSocketListener implements WebSocket.Listener {

    private final RocketChatWebSocketListener listener;

    private final JdkSocketMessageBuffer messageBuffer = new JdkSocketMessageBuffer();

    @Override
    public void onOpen(WebSocket webSocket) {
        log.info("WebSocket connection opened");
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        messageBuffer.append(data);

        if (last) {
            messageBuffer.consumeMessage(listener::onMessage);
        }

        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        log.trace("Websocket ping message received");
        return WebSocket.Listener.super.onPing(webSocket, message);
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        log.trace("Websocket pong message received");
        return WebSocket.Listener.super.onPong(webSocket, message);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        log.info("Websocket connection closed with status code {} and reason {}", statusCode, reason);
        listener.onClose(statusCode, reason);
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("Websocket error received", error);
        listener.onError(error);
        WebSocket.Listener.super.onError(webSocket, error);
    }
}

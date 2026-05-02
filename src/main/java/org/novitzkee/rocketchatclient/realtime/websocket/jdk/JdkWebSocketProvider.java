package org.novitzkee.rocketchatclient.realtime.websocket.jdk;

import lombok.Builder;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import org.novitzkee.rocketchatclient.realtime.websocket.iface.RocketChatWebSocket;
import org.novitzkee.rocketchatclient.realtime.websocket.iface.RocketChatWebSocketListener;
import org.novitzkee.rocketchatclient.realtime.websocket.iface.RocketChatWebSocketProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;

public class JdkWebSocketProvider implements RocketChatWebSocketProvider {

    private final HttpClient httpClient;

    private final URI webSocketUri;

    @Builder
    private JdkWebSocketProvider(@Nullable HttpClient httpClient, @NonNull URI webSocketUri) {
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
        this.webSocketUri = webSocketUri;
    }

    @Override
    public CompletableFuture<RocketChatWebSocket> createWebSocket(RocketChatWebSocketListener listener) {
        final JdkWebSocketListener jdkWebSocketListener = new JdkWebSocketListener(listener);
        return httpClient.newWebSocketBuilder()
                .buildAsync(webSocketUri, jdkWebSocketListener)
                .thenApply(JdkSequentialSendWebSocket::new);
    }
}

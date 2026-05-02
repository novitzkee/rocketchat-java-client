package org.novitzkee.rocketchatclient.realtime.websocket.iface;

import java.util.concurrent.CompletableFuture;

public interface RocketChatWebSocketProvider {
    CompletableFuture<RocketChatWebSocket> createWebSocket(RocketChatWebSocketListener listener);
}

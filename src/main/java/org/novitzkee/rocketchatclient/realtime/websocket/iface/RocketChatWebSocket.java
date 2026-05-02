package org.novitzkee.rocketchatclient.realtime.websocket.iface;

import java.util.concurrent.CompletableFuture;

public interface RocketChatWebSocket {

    boolean isConnectionActive();

    CompletableFuture<Void> send(String message);

    CompletableFuture<Void> close();
}

package org.novitzkee.rocketchatclient.realtime.websocket.iface;

public interface RocketChatWebSocketListener {

    void onMessage(String message);

    void onClose(int statusCode, String reason);

    void onError(Throwable error);
}

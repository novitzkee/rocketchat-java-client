package org.novitzkee.rocketchatclient.realtime.util;

import java.util.function.Consumer;

public class WebSocketMessageBuffer {

    private final StringBuilder messageBuffer = new StringBuilder();

    public void append(CharSequence data) {
        messageBuffer.append(data);
    }

    public void consumeMessage(Consumer<String> messageConsumer) {
        String completeMessage = messageBuffer.toString();
        messageBuffer.setLength(0);
        messageConsumer.accept(completeMessage);
    }
}

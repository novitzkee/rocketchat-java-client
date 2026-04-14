package org.novitzkee.rocketchatclient.realtime.exception;

public class RocketChatRealtimeClientException extends RuntimeException {

    public RocketChatRealtimeClientException(String message) {
        super(message);
    }

    public RocketChatRealtimeClientException(String message, Throwable cause) {
        super(message, cause);
    }
}

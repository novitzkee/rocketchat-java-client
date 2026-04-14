package org.novitzkee.rocketchatclient.realtime.exception;

import lombok.Getter;
import org.novitzkee.rocketchatclient.realtime.common.MethodResponse;

@Getter
public class RocketChatRealtimeMethodCallException extends RocketChatRealtimeClientException {

    private final int error;

    private final String errorType;

    private RocketChatRealtimeMethodCallException(int error, String errorType, String message) {
        super(message);
        this.error = error;
        this.errorType = errorType;
    }

    public static RocketChatRealtimeClientException fromErrorResponse(MethodResponse.Error error) {
        return new RocketChatRealtimeMethodCallException(error.error(), error.errorType(), error.message());
    }
}

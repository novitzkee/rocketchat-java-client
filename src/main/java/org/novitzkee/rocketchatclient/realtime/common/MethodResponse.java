package org.novitzkee.rocketchatclient.realtime.common;

import com.jayway.jsonpath.JsonPath;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class MethodResponse<R> implements DdpMessage {

    public static final JsonPath CALL_ID_PATH = JsonPath.compile("$.id");

    private final DdpMessageType msg = DdpMessageType.RESULT;

    private R result;

    private Error error;

    public record Error(int error, String reason, String message, String errorType) {  }
}

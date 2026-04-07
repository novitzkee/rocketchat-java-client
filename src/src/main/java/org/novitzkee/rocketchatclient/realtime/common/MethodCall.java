package org.novitzkee.rocketchatclient.realtime.common;

import com.jayway.jsonpath.JsonPath;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class MethodCall<M extends DdpMessage, R> implements SynchronousCall<M, R>, DdpMessage {

    public static final JsonPath CALL_ID_PATH = JsonPath.compile("$.id");

    @Setter
    private CallId id;

    private final DdpMessageType msg = DdpMessageType.METHOD;
}

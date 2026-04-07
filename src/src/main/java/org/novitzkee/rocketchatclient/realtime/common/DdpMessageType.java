package org.novitzkee.rocketchatclient.realtime.common;

import com.jayway.jsonpath.JsonPath;

public record DdpMessageType(String value) {

    public static final DdpMessageType CONNECT = of("connect");

    public static final DdpMessageType CONNECTED = of("connected");

    public static final DdpMessageType METHOD = of("method");

    public static final DdpMessageType RESULT = of("result");

    public static final DdpMessageType UPDATE = of("updated");

    public static final DdpMessageType PING = of("ping");

    public static final DdpMessageType PONG = of("pong");

    public static final DdpMessageType SUBSCRIBE = of("sub");

    public static final DdpMessageType UNSUBSCRIBE = of("unsub");

    public static final JsonPath DDP_MESSAGE_TYPE_PATH = JsonPath.compile("$.msg");

    public static DdpMessageType of(String value) {
        return new DdpMessageType(value);
    }
}

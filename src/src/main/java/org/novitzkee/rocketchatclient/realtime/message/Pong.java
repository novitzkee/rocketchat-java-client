package org.novitzkee.rocketchatclient.realtime.message;


import lombok.Getter;
import lombok.experimental.Accessors;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessage;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessageType;

@Getter
@Accessors(fluent = true)
public class Pong implements DdpMessage {

    @Getter
    private final DdpMessageType msg = DdpMessageType.PONG;
}

package org.novitzkee.rocketchatclient.realtime.message;


import lombok.Getter;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessage;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessageType;

@Getter
public class Pong implements DdpMessage {

    @Getter
    private final DdpMessageType msg = DdpMessageType.PONG;
}

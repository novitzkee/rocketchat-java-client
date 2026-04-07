package org.novitzkee.rocketchatclient.realtime.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessage;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessageType;

@RequiredArgsConstructor
@Getter
public class Connected implements DdpMessage {

    private final DdpMessageType msg;

    private final String session;

}

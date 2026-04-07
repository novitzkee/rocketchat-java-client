package org.novitzkee.rocketchatclient.realtime.method;

import org.novitzkee.rocketchatclient.realtime.common.DdpMessage;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessageType;

public class LoginMethodResult implements DdpMessage {

    @Override
    public DdpMessageType getMsg() {
        return DdpMessageType.RESULT;
    }
}

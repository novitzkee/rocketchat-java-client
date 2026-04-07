package org.novitzkee.rocketchatclient.realtime.message;

import lombok.Getter;
import org.novitzkee.rocketchatclient.realtime.common.CallId;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessage;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessageType;
import org.novitzkee.rocketchatclient.realtime.common.SynchronousCall;
import org.novitzkee.rocketchatclient.realtime.util.CallUtil;

@Getter
public class Connect implements SynchronousCall<Connected, String>, DdpMessage {

    public static final CallId CONNECT_MSG_ID = CallId.of(0);

    private final DdpMessageType msg = DdpMessageType.CONNECT;

    private final String version = "1";

    private final String[] support = {"1"};

    @Override
    public CallId getId() {
        return CONNECT_MSG_ID;
    }

    @Override
    public Class<Connected> getResponseMessageClass() {
        return Connected.class;
    }

    @Override
    public String getResult(Connected connectedMessage) {
        CallUtil.expectMessageOfType(connectedMessage, DdpMessageType.CONNECTED);
        return connectedMessage.getSession();
    }
}

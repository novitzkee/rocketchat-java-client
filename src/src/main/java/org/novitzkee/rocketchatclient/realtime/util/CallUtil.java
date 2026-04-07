package org.novitzkee.rocketchatclient.realtime.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessage;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessageType;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.NONE)
public class CallUtil {

    public static void expectMessageOfType(DdpMessage ddpMessage, DdpMessageType expectedType) {
        if (!Objects.equals(ddpMessage.getMsg(), expectedType)) {
            throw new IllegalStateException("Unexpected response message type");
        }
    }
}

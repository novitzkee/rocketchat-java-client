package org.novitzkee.rocketchatclient.realtime.message;

import org.novitzkee.rocketchatclient.realtime.common.DdpMessage;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessageType;

public record Connected(DdpMessageType msg, String session) implements DdpMessage {

}

package org.novitzkee.rocketchatclient.realtime.json;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessageType;

public class MessageTypeAdapter {

    @ToJson
    String toJson(DdpMessageType ddpMessageType) {
        return ddpMessageType.value();
    }

    @FromJson
    DdpMessageType fromJson(String ddpMessageType) {
        return DdpMessageType.of(ddpMessageType);
    }
}

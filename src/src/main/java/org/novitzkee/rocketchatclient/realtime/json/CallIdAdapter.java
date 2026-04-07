package org.novitzkee.rocketchatclient.realtime.json;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;
import org.novitzkee.rocketchatclient.realtime.common.CallId;

public class CallIdAdapter {

    @ToJson
    String toJson(CallId callId) {
        return Integer.toString(callId.value());
    }

    @FromJson
    CallId fromJson(String callIdString) {
        return CallId.of(Integer.parseInt(callIdString));
    }
}

package org.novitzkee.rocketchatclient.realtime.json;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;
import org.novitzkee.rocketchatclient.realtime.common.MethodName;

public class MethodNameAdapter {

    @ToJson
    String toJson(MethodName methodName) {
        return methodName.value();
    }

    @FromJson
    MethodName fromJson(String methodName) {
        return MethodName.of(methodName);
    }
}

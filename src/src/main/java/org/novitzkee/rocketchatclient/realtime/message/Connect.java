package org.novitzkee.rocketchatclient.realtime.message;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.novitzkee.rocketchatclient.realtime.common.CallId;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessage;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessageType;
import org.novitzkee.rocketchatclient.realtime.common.SynchronousCall;

@Getter
@Accessors(fluent = true, makeFinal = true)
@RequiredArgsConstructor
public class Connect implements SynchronousCall<Connected, String>, DdpMessage {

    public static final CallId CONNECT_MSG_ID = CallId.of(0);

    private final DdpMessageType msg = DdpMessageType.CONNECT;

    private final String version = "1";

    private final String[] support = {"1"};

    @Override
    public CallId id() {
        return CONNECT_MSG_ID;
    }

    @Override
    public JsonAdapter<Connected> responseJsonAdapter(Moshi moshi) {
        return moshi.adapter(Connected.class);
    }

    @Override
    public String getResult(Connected connectedMessage) {
        return connectedMessage.session();
    }
}

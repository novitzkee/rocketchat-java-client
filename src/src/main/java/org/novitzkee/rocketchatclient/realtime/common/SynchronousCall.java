package org.novitzkee.rocketchatclient.realtime.common;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

public interface SynchronousCall<M extends DdpMessage, R> {

    CallId id();

    JsonAdapter<M> responseJsonAdapter(Moshi moshi);

    R getResult(M response);
}

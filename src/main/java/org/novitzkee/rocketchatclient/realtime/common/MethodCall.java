package org.novitzkee.rocketchatclient.realtime.common;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.novitzkee.rocketchatclient.realtime.exception.RocketChatRealtimeMethodCallException;

import java.lang.reflect.Type;

@Getter
@Accessors(fluent = true, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class MethodCall<R> implements SynchronousCall<MethodResponse<R>, R>, DdpMessage {

    private final DdpMessageType msg = DdpMessageType.METHOD;

    @Setter
    private CallId id;

    private final MethodName method;

    private final Object[] params;

    @Override
    public final JsonAdapter<MethodResponse<R>> responseJsonAdapter(Moshi moshi) {
        final Type fullResponseType = Types.newParameterizedType(MethodResponse.class, resultClass());
        return moshi.adapter(fullResponseType);
    }

    @Override
    public final R getResult(MethodResponse<R> responseMessage) {
        if (responseMessage.error() != null) {
            throw RocketChatRealtimeMethodCallException.fromErrorResponse(responseMessage.error());
        }

        return responseMessage.result();
    }

    protected abstract Class<R> resultClass();
}

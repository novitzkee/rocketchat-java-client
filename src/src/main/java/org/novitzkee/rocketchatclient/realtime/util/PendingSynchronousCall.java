package org.novitzkee.rocketchatclient.realtime.util;

import com.squareup.moshi.Moshi;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.novitzkee.rocketchatclient.realtime.common.CallId;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessage;
import org.novitzkee.rocketchatclient.realtime.common.SynchronousCall;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class PendingSynchronousCall<M extends DdpMessage, R> {

    private final SynchronousCall<M, R> call;

    private final CompletableFuture<R> result;

    public static <M extends DdpMessage, T> PendingSynchronousCall<M, T> start(SynchronousCall<M, T> call) {
        log.debug("STARTING method call with id: {} and class: {}", call.getId(), call.getClass().getSimpleName());
        return new PendingSynchronousCall<>(call, new CompletableFuture<>());
    }

    public void complete(String receivedJson, Moshi moshi) {
        try {
            final M message = moshi.adapter(call.getResponseMessageClass()).fromJson(receivedJson);
            final R result = call.getResult(message);
            this.result.complete(result);
            log.debug("COMPLETING method call with class: {} with id: {} with result: {}", getCallClassName(), call.getId(), result);
        } catch (Exception e) {
            log.debug("COMPLETING method call with class: {} with id: {} with exception", getCallClassName(), call.getId(), e);
            result.completeExceptionally(e);
        }
    }

    public void completeExceptionally(Exception e) {
        log.debug("COMPLETING method call with class: {} with id: {} with exception", getCallClassName(), call.getId(), e);
        result.completeExceptionally(e);
    }

    public CallId getId() {
        return call.getId();
    }

    public String getCallClassName() {
        return call.getClass()
                .getSimpleName();
    }
}

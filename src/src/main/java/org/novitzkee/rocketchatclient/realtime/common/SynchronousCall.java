package org.novitzkee.rocketchatclient.realtime.common;

public interface SynchronousCall<M extends DdpMessage, R> extends DdpMessage {

    CallId getId();

    Class<M> getResponseMessageClass();

    R getResult(M responseMessage);
}

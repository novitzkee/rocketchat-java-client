package org.novitzkee.rocketchatclient.realtime.common;

public record MethodName(String value) {

    public static final MethodName LOGIN = MethodName.of("login");

    public static MethodName of(String value) {
        return new MethodName(value);
    }
}

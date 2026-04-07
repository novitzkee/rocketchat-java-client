package org.novitzkee.rocketchatclient.realtime.common;

public record CallId(int value) {

    public static CallId of(int value) {
        return new CallId(value);
    }

    public static CallId fromString(String value) {
        return of(Integer.parseInt(value));
    }
}

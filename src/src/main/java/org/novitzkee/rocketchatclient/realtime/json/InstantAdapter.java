package org.novitzkee.rocketchatclient.realtime.json;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.Json;
import com.squareup.moshi.ToJson;

import java.time.Instant;

public class InstantAdapter {

    @FromJson
    Instant fromJson(InstantJson json) {
        if (json == null) return null;
        return Instant.ofEpochMilli(json.date);
    }

    @ToJson
    InstantJson toJson(Instant instant) {
        return new InstantJson(instant.toEpochMilli());
    }

    public record InstantJson(@Json(name = "$date") long date) { }
}

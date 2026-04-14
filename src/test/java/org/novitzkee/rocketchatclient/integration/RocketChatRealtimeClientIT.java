package org.novitzkee.rocketchatclient.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.novitzkee.rocketchatclient.realtime.RocketChatRealtimeClient;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class RocketChatRealtimeClientIT extends RocketChatIntegrationTest {

    private static final String ROCKET_CHAT_REALTIME_API_PATH = "websocket";

    private final RocketChatRealtimeClient rocketChatRealtimeClient = RocketChatRealtimeClient.builder()
            .apiUri(rocketChatWebsocketUrl().resolve(ROCKET_CHAT_REALTIME_API_PATH))
            .callTimeoutDuration(Duration.ofSeconds(15))
            .httpClient(
                    HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_1_1)
                            .build()
            )
            .build();


    @Test
    void testConnect() {
        // when
        final String session = rocketChatRealtimeClient.connect()
                .join();

        // then
        log.info("Login result: {}", session);
        assertThat(session).isNotEmpty();
    }
}

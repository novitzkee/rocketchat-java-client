package org.novitzkee.rocketchatclient.integration;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Work in progress.
 */
public class RocketChatRestClientIT extends RocketChatIntegrationTest {

    @Test
    void shouldRetrieveAPIStatus() throws Exception {
        // given
        final HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        final URI requestUri = rocketChatHttpUrl().resolve("api/info");
        final HttpRequest apiInfoRequest = HttpRequest.newBuilder(requestUri)
                .GET()
                .build();

        // when
        final HttpResponse<String> response = httpClient.send(apiInfoRequest, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.statusCode()).isEqualTo(200);
    }
}

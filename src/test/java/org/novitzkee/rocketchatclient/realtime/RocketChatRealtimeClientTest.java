package org.novitzkee.rocketchatclient.realtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessageType;
import org.novitzkee.rocketchatclient.realtime.util.RocketChatRealtimeClientException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.novitzkee.rocketchatclient.realtime.RocketChatRealtimeTestResponses.OK_CONNECT_MESSAGE;
import static org.novitzkee.rocketchatclient.realtime.RocketChatRealtimeTestResponses.PING_MESSAGE;

class RocketChatRealtimeClientTest {

    private static final String CONNECT_MESSAGE = "{\"msg\":\"connect\",\"support\":[\"1\"],\"version\":\"1\"}";

    private static final String PONG_MESSAGE = "{\"msg\":\"pong\"}";

    private static final Executor SMALL_DELAY_EXECUTOR = CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS, Executors.newSingleThreadExecutor());

    private static final Executor LONG_DELAY_EXECUTOR = CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS, Executors.newSingleThreadExecutor());

    private static final Duration TEST_TIMEOUT_DURATION = Duration.ofMillis(200);

    private static final URI TEST_API_URL = URI.create("http://localhost:1234/websocket");

    private static final String CALL_TIMED_OUT = "call timed out";

    private WebSocket webSocketMock;

    private RocketChatRealtimeClient rocketChatRealtimeClient;

    private RocketChatRealtimeClient.RocketChatWebSocketListener rocketChatWebSocketListener;

    @BeforeEach
    void setUp() {
        this.webSocketMock = createWebSocketMock();
        this.rocketChatRealtimeClient = RocketChatRealtimeClient.builder()
                .apiUri(TEST_API_URL)
                .callTimeoutDuration(TEST_TIMEOUT_DURATION)
                .httpClient(createHttpClientMock())
                .build();

        this.rocketChatWebSocketListener = rocketChatRealtimeClient.getRocketChatWebSocketListener();
    }

    @Test
    void shouldConnectToRocketChatRealtimeAPI() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        setUpOkConnectResponse();

        // when
        rocketChatRealtimeClient.connect()
                .get(1L, TimeUnit.SECONDS);

        // then
        final ArgumentCaptor<CharSequence> sentMessageCaptor = ArgumentCaptor.forClass(CharSequence.class);
        verify(webSocketMock).sendText(sentMessageCaptor.capture(), eq(true));

        final List<CharSequence> sentMessages = sentMessageCaptor.getAllValues();

        assertThat(sentMessages).hasSize(1)
                .satisfiesExactly(
                        firstMessage -> assertThat(firstMessage).isEqualToIgnoringWhitespace(CONNECT_MESSAGE)
                );
    }

    @Test
    void shouldCompleteWithClientExceptionWhenConnectResponseNotReceivedWithinSpecifiedTimeout() {
        // given
        setUpDelayedConnectResponse();

        // when
        final CompletableFuture<String> connectFuture = rocketChatRealtimeClient.connect();

        // then
        await().atMost(2L, TimeUnit.SECONDS)
                .until(connectFuture::isDone);

        assertThatThrownBy(connectFuture::join).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RocketChatRealtimeClientException.class)
                .hasMessageContaining(CALL_TIMED_OUT);
    }

    @Test
    void shouldSendPongMessageWhenPingReceivedAfterConnectionEstablished() throws Exception {
        // given
        setUpOkConnectResponse();
        rocketChatRealtimeClient.connect().get(1L, TimeUnit.SECONDS);

        // when
        rocketChatWebSocketListener.onText(webSocketMock, PING_MESSAGE, true);

        // then
        final ArgumentCaptor<CharSequence> sentMessageCaptor = ArgumentCaptor.forClass(CharSequence.class);
        verify(webSocketMock, times(2)).sendText(sentMessageCaptor.capture(), eq(true));

        final List<CharSequence> sentMessages = sentMessageCaptor.getAllValues();

        assertThat(sentMessages).hasSize(2)
                .satisfiesExactly(
                        firstMessage -> assertThat(firstMessage).isEqualToIgnoringWhitespace(CONNECT_MESSAGE),
                        secondMessage -> assertThat(secondMessage).isEqualToIgnoringWhitespace(PONG_MESSAGE)
                );
    }

    private void setUpOkConnectResponse() {
        when(webSocketMock.sendText(contains(DdpMessageType.CONNECT.value()), eq(true))).thenAnswer(ignored -> {
            SMALL_DELAY_EXECUTOR.execute(() -> rocketChatWebSocketListener.onText(webSocketMock, OK_CONNECT_MESSAGE, true));
            return CompletableFuture.completedFuture(webSocketMock);
        });
    }

    private void setUpDelayedConnectResponse() {
        when(webSocketMock.sendText(contains(DdpMessageType.CONNECT.value()), eq(true))).thenAnswer(ignored -> {
            LONG_DELAY_EXECUTOR.execute(() -> rocketChatWebSocketListener.onText(webSocketMock, OK_CONNECT_MESSAGE, true));
            return CompletableFuture.completedFuture(webSocketMock);
        });
    }

    private WebSocket createWebSocketMock() {
        final WebSocket wsMock = mock(WebSocket.class);
        when(wsMock.isInputClosed()).thenReturn(false);
        when(wsMock.isOutputClosed()).thenReturn(false);
        when(wsMock.sendText(any(), anyBoolean())).thenReturn(CompletableFuture.completedFuture(wsMock));
        return wsMock;
    }

    private HttpClient createHttpClientMock() {
        final WebSocket.Builder webSocketBuilderMock = mock(WebSocket.Builder.class);
        when(webSocketBuilderMock.buildAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(webSocketMock));

        final HttpClient httpClientMock = mock(HttpClient.class);
        when(httpClientMock.newWebSocketBuilder()).thenReturn(webSocketBuilderMock);
        return httpClientMock;
    }
}

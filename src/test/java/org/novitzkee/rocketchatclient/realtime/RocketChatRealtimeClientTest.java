package org.novitzkee.rocketchatclient.realtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.novitzkee.rocketchatclient.realtime.common.DdpMessageType;
import org.novitzkee.rocketchatclient.realtime.common.MethodName;
import org.novitzkee.rocketchatclient.realtime.common.MethodResponse;
import org.novitzkee.rocketchatclient.realtime.exception.RocketChatRealtimeClientException;
import org.novitzkee.rocketchatclient.realtime.exception.RocketChatRealtimeMethodCallException;
import org.novitzkee.rocketchatclient.realtime.method.authentication.LoginMethodCall;

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
import static org.novitzkee.rocketchatclient.realtime.RocketChatRealtimeMessages.*;
import static org.novitzkee.rocketchatclient.util.MessageMatchers.ddpMessageOfType;
import static org.novitzkee.rocketchatclient.util.MessageMatchers.methodCallWithName;

class RocketChatRealtimeClientTest {

    private static final Duration TEST_TIMEOUT_DURATION = Duration.ofMillis(200);

    private static final Duration FAST_RESPONSE_DELAY = TEST_TIMEOUT_DURATION.dividedBy(4);

    private static final Duration OVERDUE_RESPONSE_DELAY = TEST_TIMEOUT_DURATION.multipliedBy(10);

    private static final Executor SMALL_DELAY_EXECUTOR = CompletableFuture.delayedExecutor(
            FAST_RESPONSE_DELAY.toMillis(),
            TimeUnit.MILLISECONDS,
            Executors.newSingleThreadExecutor()
    );

    private static final Executor OVERDUE_DELAY_EXECUTOR = CompletableFuture.delayedExecutor(
            OVERDUE_RESPONSE_DELAY.toMillis(),
            TimeUnit.MILLISECONDS,
            Executors.newSingleThreadExecutor()
    );

    private static final Duration TIMEOUT_ASSERTION_WAIT = OVERDUE_RESPONSE_DELAY.multipliedBy(2);

    private static final Duration FAST_CALL_GET_TIMEOUT = FAST_RESPONSE_DELAY.multipliedBy(10);

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
    void shouldConnectToRocketChatRealtimeAPI() throws Exception {
        // given
        setUpConnectResponse(SMALL_DELAY_EXECUTOR);

        // when
        rocketChatRealtimeClient.connect().get(FAST_CALL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

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
        setUpConnectResponse(OVERDUE_DELAY_EXECUTOR);

        // when
        final CompletableFuture<String> connectFuture = rocketChatRealtimeClient.connect();

        // then
        await().atMost(TIMEOUT_ASSERTION_WAIT.toMillis(), TimeUnit.MILLISECONDS)
                .until(connectFuture::isDone);

        assertThatThrownBy(connectFuture::join).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RocketChatRealtimeClientException.class)
                .hasMessageContaining(CALL_TIMED_OUT);
    }

    @Test
    void shouldSendPongMessageWhenPingReceivedAfterConnectionEstablished() throws Exception {
        // given
        setUpConnectResponse(SMALL_DELAY_EXECUTOR);
        rocketChatRealtimeClient.connect().get(FAST_CALL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

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

    @Test
    void shouldLoginToRealtimeAPI() throws Exception {
        // given
        setUpConnectResponse(SMALL_DELAY_EXECUTOR);
        setUpMethodResponse(SMALL_DELAY_EXECUTOR, MethodName.LOGIN, RocketChatRealtimeMessages::loginOkResponse);

        final LoginMethodCall loginMethodCall = LoginMethodCall.usingAuthenticationToken("test-token");

        // when
        rocketChatRealtimeClient.connect().get(FAST_CALL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        final LoginMethodCall.Info result = rocketChatRealtimeClient.performMethodCall(loginMethodCall).get(FAST_CALL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        // then
        assertThat(result.id()).isNotBlank();
        assertThat(result.token()).isNotBlank();
        assertThat(result.tokenExpires()).isNotNull();
        assertThat(result.type()).isNotNull();
    }

    @Test
    void shouldCompleteLoginWithMethodCallExceptionWhenErrorIsReceived() throws Exception {
        // given
        setUpConnectResponse(SMALL_DELAY_EXECUTOR);
        setUpMethodResponse(SMALL_DELAY_EXECUTOR, MethodName.LOGIN, RocketChatRealtimeMessages::loginErrorResponse);

        final LoginMethodCall loginMethodCall = LoginMethodCall.usingAuthenticationToken("test-token");

        // when
        rocketChatRealtimeClient.connect().get(FAST_CALL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        final CompletableFuture<LoginMethodCall.Info> loginFuture = rocketChatRealtimeClient.performMethodCall(loginMethodCall);

        // then
        await().atMost(TIMEOUT_ASSERTION_WAIT.toMillis(), TimeUnit.MILLISECONDS)
                .until(loginFuture::isDone);

        assertThatThrownBy(loginFuture::join).isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOfSatisfying(
                        RocketChatRealtimeMethodCallException.class,
                        e -> {
                            assertThat(e.getError()).isEqualTo(403);
                            assertThat(e.getErrorType()).isEqualTo("Meteor.Error");
                            assertThat(e.getMessage()).contains("Please log in again");
                        }
                );
    }

    private void setUpConnectResponse(Executor executor) {
        when(webSocketMock.sendText(ddpMessageOfType(DdpMessageType.CONNECT), eq(true))).thenAnswer(ignored -> {
            executor.execute(() -> rocketChatWebSocketListener.onText(webSocketMock, CONNECTED_MESSAGE, true));
            return CompletableFuture.completedFuture(webSocketMock);
        });
    }

    private void setUpMethodResponse(Executor executor, MethodName calledMethodName, ResponseProvider provider) {
        when(webSocketMock.sendText(methodCallWithName(calledMethodName), eq(true))).thenAnswer(invocation -> {
            final String request = invocation.getArgument(0);
            final String callId = MethodResponse.CALL_ID_PATH.read(request);
            executor.execute(() -> rocketChatWebSocketListener.onText(webSocketMock, provider.createForId(callId), true));
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

    @FunctionalInterface
    private interface ResponseProvider {
        String createForId(String callId);
    }
}

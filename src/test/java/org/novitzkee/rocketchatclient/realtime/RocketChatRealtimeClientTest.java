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
import org.novitzkee.rocketchatclient.realtime.websocket.iface.RocketChatWebSocket;
import org.novitzkee.rocketchatclient.realtime.websocket.iface.RocketChatWebSocketListener;
import org.novitzkee.rocketchatclient.realtime.websocket.iface.RocketChatWebSocketProvider;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
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

    private static final String CALL_TIMED_OUT = "call timed out";

    private RocketChatRealtimeClient rocketChatRealtimeClient;

    private RocketChatWebSocket rocketChatWebSocketMock;

    private RocketChatWebSocketListener rocketChatWebSocketListener;

    @BeforeEach
    void setUp() {
        this.rocketChatWebSocketMock = mock(RocketChatWebSocket.class);
        when(rocketChatWebSocketMock.isConnectionActive()).thenReturn(true);

        this.rocketChatRealtimeClient = RocketChatRealtimeClient.builder()
                .callTimeoutDuration(TEST_TIMEOUT_DURATION)
                .webSocketProvider(new TestWebSocketProvider())
                .build();
    }

    @Test
    void shouldCompleteWithClientExceptionWhenNotConnected() {
        // given
        final LoginMethodCall loginMethodCall = LoginMethodCall.usingAuthenticationToken("test-token");

        // when
        final CompletableFuture<?> loginFuture = rocketChatRealtimeClient.performMethodCall(loginMethodCall);

        // then
        assertThatThrownBy(loginFuture::join).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RocketChatRealtimeClientException.class)
                .hasMessageContaining("not connected");
    }

    @Test
    void shouldConnectToRocketChatRealtimeAPI() throws Exception {
        // given
        setUpConnectResponse(SMALL_DELAY_EXECUTOR);

        // when
        rocketChatRealtimeClient.connect().get(FAST_CALL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        // then
        final ArgumentCaptor<String> sentMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketChatWebSocketMock).send(sentMessageCaptor.capture());

        final List<String> sentMessages = sentMessageCaptor.getAllValues();

        assertThat(sentMessages).hasSize(1)
                .satisfiesExactly(
                        firstMessage -> assertThat(firstMessage).isEqualToIgnoringWhitespace(CONNECT_MESSAGE)
                );
    }

    @Test
    void shouldSendPongMessageWhenPingReceivedAfterConnectionEstablished() throws Exception {
        // given
        setUpConnectResponse(SMALL_DELAY_EXECUTOR);
        rocketChatRealtimeClient.connect().get(FAST_CALL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        // when
        rocketChatWebSocketListener.onMessage(PING_MESSAGE);

        // then
        await().atMost(FAST_RESPONSE_DELAY.toMillis(), TimeUnit.MILLISECONDS)
                .pollInterval(FAST_RESPONSE_DELAY.toMillis() / 5, TimeUnit.MILLISECONDS)
                .untilAsserted(this::assertConnectAndPongMessageSent);
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
    void shouldCompleteWithClientExceptionWhenLoginMethodResponseNotReceivedWithinSpecifiedTimeout() throws Exception {
        // given
        setUpConnectResponse(SMALL_DELAY_EXECUTOR);
        setUpMethodResponse(OVERDUE_DELAY_EXECUTOR, MethodName.LOGIN, RocketChatRealtimeMessages::loginOkResponse);

        final LoginMethodCall loginMethodCall = LoginMethodCall.usingAuthenticationToken("test-token");

        // when
        rocketChatRealtimeClient.connect().get(FAST_CALL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        final CompletableFuture<?> loginFuture = rocketChatRealtimeClient.performMethodCall(loginMethodCall);

        // then
        await().atMost(TIMEOUT_ASSERTION_WAIT.toMillis(), TimeUnit.MILLISECONDS)
                .until(loginFuture::isDone);

        assertThatThrownBy(loginFuture::join).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RocketChatRealtimeClientException.class)
                .hasMessageContaining(CALL_TIMED_OUT);
    }

    @Test
    void shouldNotAcceptNewCallsAndFailAllPendingCallsWhenClosed() throws Exception {
        // given
        setUpConnectResponse(SMALL_DELAY_EXECUTOR);
        setUpMethodResponse(OVERDUE_DELAY_EXECUTOR, MethodName.LOGIN, RocketChatRealtimeMessages::loginOkResponse);

        final LoginMethodCall loginMethodCall = LoginMethodCall.usingAuthenticationToken("test-token");

        // when
        rocketChatRealtimeClient.connect().get(FAST_CALL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        final CompletableFuture<?> beforeCloseResult = rocketChatRealtimeClient.performMethodCall(loginMethodCall);
        rocketChatRealtimeClient.close().get(FAST_CALL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        final CompletableFuture<?> afterCloseResult = rocketChatRealtimeClient.performMethodCall(loginMethodCall);

        // then
        await().atMost(FAST_RESPONSE_DELAY.toMillis(), TimeUnit.MILLISECONDS)
                .pollInterval(FAST_RESPONSE_DELAY.toMillis() / 5, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(rocketChatWebSocketMock).close());

        await().atMost(FAST_RESPONSE_DELAY.toMillis(), TimeUnit.MILLISECONDS)
                .pollInterval(FAST_RESPONSE_DELAY.toMillis() / 5, TimeUnit.MILLISECONDS)
                .until(() -> beforeCloseResult.isDone() && afterCloseResult.isDone());

        assertThatThrownBy(beforeCloseResult::join).isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOfSatisfying(
                        RocketChatRealtimeClientException.class,
                        e -> assertThat(e.getMessage()).contains("Client closing")
                );

        assertThatThrownBy(afterCloseResult::join).isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOfSatisfying(
                        RocketChatRealtimeClientException.class,
                        e -> assertThat(e.getMessage()).contains("not connected")
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
        final LoginMethodCall.Info result = rocketChatRealtimeClient.performMethodCall(loginMethodCall)
                .get(FAST_CALL_GET_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

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
        when(rocketChatWebSocketMock.send(ddpMessageOfType(DdpMessageType.CONNECT))).thenAnswer(ignored -> {
            executor.execute(() -> rocketChatWebSocketListener.onMessage(CONNECTED_MESSAGE));
            return CompletableFuture.completedFuture(rocketChatWebSocketMock);
        });
    }

    private void setUpMethodResponse(Executor executor, MethodName calledMethodName, ResponseProvider provider) {
        when(rocketChatWebSocketMock.send(methodCallWithName(calledMethodName))).thenAnswer(invocation -> {
            final String request = invocation.getArgument(0);
            final String callId = MethodResponse.CALL_ID_PATH.read(request);
            executor.execute(() -> rocketChatWebSocketListener.onMessage(provider.createForId(callId)));
            return CompletableFuture.completedFuture(rocketChatWebSocketMock);
        });
    }

    private void assertConnectAndPongMessageSent() {
        final ArgumentCaptor<String> sentMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketChatWebSocketMock, times(2)).send(sentMessageCaptor.capture());
        assertThat(sentMessageCaptor.getAllValues()).hasSize(2)
                .satisfiesExactly(
                        firstMessage -> assertThat(firstMessage).isEqualToIgnoringWhitespace(CONNECT_MESSAGE),
                        secondMessage -> assertThat(secondMessage).isEqualToIgnoringWhitespace(PONG_MESSAGE)
                );
    }

    @FunctionalInterface
    private interface ResponseProvider {
        String createForId(String callId);
    }

    private class TestWebSocketProvider implements RocketChatWebSocketProvider {

        @Override
        public CompletableFuture<RocketChatWebSocket> createWebSocket(RocketChatWebSocketListener listener) {
            rocketChatWebSocketListener = listener;
            return CompletableFuture.completedFuture(rocketChatWebSocketMock);
        }
    }
}

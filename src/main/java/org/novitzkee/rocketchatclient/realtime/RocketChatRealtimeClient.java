package org.novitzkee.rocketchatclient.realtime;

import com.github.benmanes.caffeine.cache.*;
import com.squareup.moshi.Moshi;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.novitzkee.rocketchatclient.realtime.common.*;
import org.novitzkee.rocketchatclient.realtime.json.CallIdAdapter;
import org.novitzkee.rocketchatclient.realtime.json.InstantAdapter;
import org.novitzkee.rocketchatclient.realtime.json.MessageTypeAdapter;
import org.novitzkee.rocketchatclient.realtime.json.MethodNameAdapter;
import org.novitzkee.rocketchatclient.realtime.message.Connect;
import org.novitzkee.rocketchatclient.realtime.message.Pong;
import org.novitzkee.rocketchatclient.realtime.util.PendingSynchronousCall;
import org.novitzkee.rocketchatclient.realtime.exception.RocketChatRealtimeClientException;
import org.novitzkee.rocketchatclient.realtime.util.WebSocketMessageBuffer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client for Rocket.Chat Realtime API (DDP protocol).
 *
 * @see <a href="https://developer.rocket.chat/apidocs/realtimeapi">Rocket.Chat Realtime API</a>
 */
@Slf4j
// TODO: Review concurrency correctness in more detail.
// TODO: Add recovery policy for connection failures.
public class RocketChatRealtimeClient {

    private static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(10);

    private static final String CLIENT_CLOSING_MESSAGE = "Client closing";

    private static final Moshi MOSHI = new Moshi.Builder()
            .add(new CallIdAdapter())
            .add(new InstantAdapter())
            .add(new MessageTypeAdapter())
            .add(new MethodNameAdapter())
            .build();

    private final URI apiUrl;

    private final Cache<@NonNull CallId, PendingSynchronousCall<?, ?>> synchronousCallsInProgress;

    private final HttpClient httpClient;

    private final AtomicInteger idCounter = new AtomicInteger();

    private final Semaphore connectMutex = new Semaphore(1);

    @Getter(AccessLevel.PACKAGE)
    private final RocketChatWebSocketListener rocketChatWebSocketListener = new RocketChatWebSocketListener();

    @Setter(AccessLevel.PRIVATE)
    private WebSocket webSocket;

    private volatile Exception connectionFailure;

    @Builder
    private RocketChatRealtimeClient(
            @NonNull URI apiUri,
            @Nullable HttpClient httpClient,
            @Nullable Duration callTimeoutDuration
    ) {
        this.apiUrl = apiUri;
        this.httpClient = httpClient == null ? HttpClient.newHttpClient() : httpClient;
        this.synchronousCallsInProgress = Caffeine.newBuilder()
                .expireAfterWrite(callTimeoutDuration == null ? DEFAULT_CALL_TIMEOUT : callTimeoutDuration)
                .maximumSize(1_000)
                .removalListener(new PendingCallRemovalListener())
                .scheduler(Scheduler.systemScheduler())
                .build();
    }

    public boolean isConnectionActive() {
        return webSocket != null && !webSocket.isInputClosed() && !webSocket.isOutputClosed();
    }

    @SneakyThrows
    public CompletableFuture<String> connect() {
        // TODO: Verify if mutex will always be released.
        connectMutex.acquire();
        return doConnect().whenComplete((v, e) -> connectMutex.release());
    }

    public <T> CompletableFuture<T> performMethodCall(MethodCall<T> methodCall) {
        methodCall.id(CallId.of(idCounter.incrementAndGet()));
        return performCall(methodCall);
    }

    public CompletableFuture<Void> close() {
        log.info("Closing connection");

        notifyConnectionError(connectionClosedByClientException());

        final WebSocket ws = webSocket;
        if (ws == null) {
            return CompletableFuture.completedFuture(null);
        }

        setWebSocket(null);
        return ws.sendClose(WebSocket.NORMAL_CLOSURE, CLIENT_CLOSING_MESSAGE)
                .thenApply(ignored -> null);
    }

    private CompletableFuture<String> doConnect() {
        if (webSocket == null) {
            log.info("Connection not established, creating new one");
            return resetConnection();
        }

        if (!isConnectionActive()) {
            log.info("Connection is not active, creating new one");
            return resetConnection();
        }

        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<String> resetConnection() {
        connectionFailure = null;
        return establishWebSocketConnection().thenCompose(nothing -> establishDdpConnection());
    }

    private CompletableFuture<Void> establishWebSocketConnection() {
        return httpClient.newWebSocketBuilder()
                .buildAsync(apiUrl, rocketChatWebSocketListener)
                .thenAccept(this::setWebSocket);
    }

    private CompletableFuture<String> establishDdpConnection() {
        return performCall(new Connect());
    }

    private <T> CompletableFuture<T> performCall(SynchronousCall<?, T> call) {
        final String outgoingMessage = MOSHI.<SynchronousCall<?, ?>>adapter(call.getClass()).toJson(call);

        final WebSocket ws = webSocket;
        if (ws == null) {
            throw clientNotConnected();
        }

        final PendingSynchronousCall<?, T> pendingCall = PendingSynchronousCall.start(call);

        final Exception failure = connectionFailure;
        if (failure != null) {
            pendingCall.completeExceptionally(failure);
            return pendingCall.getResult();
        }

        synchronousCallsInProgress.put(pendingCall.getId(), pendingCall);

        log.trace("Sending message: {}", outgoingMessage);
        ws.sendText(outgoingMessage, true).join();

        return pendingCall.getResult();
    }

    private void receive(String message) {
        final String msg = DdpMessageType.DDP_MESSAGE_TYPE_PATH.read(message);
        final DdpMessageType ddpMessageType = DdpMessageType.of(msg);

        if (DdpMessageType.PING.equals(ddpMessageType)) {
            sendPong();
        } else if (DdpMessageType.CONNECTED.equals(ddpMessageType)) {
            finishPendingCall(Connect.CONNECT_MSG_ID, message);
        } else if (DdpMessageType.RESULT.equals(ddpMessageType)) {
            final String id = MethodResponse.CALL_ID_PATH.read(message);
            final CallId callId = CallId.fromString(id);
            finishPendingCall(callId, message);
        }
    }

    private void finishPendingCall(CallId callId, String receivedJson) {
        Optional.ofNullable(
                synchronousCallsInProgress.asMap().remove(callId)
        ).ifPresentOrElse(
                pendingCall -> pendingCall.complete(receivedJson, MOSHI),
                () -> log.trace("Received message for call not present in cache: {}", receivedJson)
        );
    }

    private void sendPong() {
        if (isConnectionActive()) {
            final String pong = MOSHI.adapter(Pong.class).toJson(new Pong());
            log.trace("Sending pong message");
            webSocket.sendText(pong, true).join();
        }
    }

    private void notifyConnectionError(Exception e) {
        connectionFailure = e;
        synchronousCallsInProgress.asMap().forEach((id, call) -> call.completeExceptionally(e));
        synchronousCallsInProgress.invalidateAll();
    }

    private static class PendingCallRemovalListener implements RemovalListener<@NonNull Object, @NonNull PendingSynchronousCall<?, ?>> {

        @Override
        public void onRemoval(
                @Nullable Object key,
                @Nullable PendingSynchronousCall<?, ?> call,
                @NonNull RemovalCause cause
        ) {
            if (call == null) {
                log.warn("Pending call entry was null in removal notification with id: {}", key);
                return;
            }

            if (RemovalCause.EXPLICIT.equals(cause)) {
                log.trace("Pending call with id: {} removed explicitly", call.getId());
                return;
            }

            final RocketChatRealtimeClientException callException = switch (cause) {
                case REPLACED -> duplicateCallId();
                case SIZE -> pendingCallLimitReached();
                case EXPIRED -> callTimedOutException();
                default -> unexpectedRemovedCall();
            };

            call.completeExceptionally(callException);
        }
    }

    class RocketChatWebSocketListener implements WebSocket.Listener {

        private final WebSocketMessageBuffer messageBuffer = new WebSocketMessageBuffer();

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("WebSocket connection opened");
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer.append(data);

            if (last) {
                messageBuffer.consumeMessage(RocketChatRealtimeClient.this::receive);
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            log.trace("Websocket ping message received");
            return WebSocket.Listener.super.onPing(webSocket, message);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            log.trace("Websocket pong message received");
            return WebSocket.Listener.super.onPong(webSocket, message);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.info("Websocket connection closed with status code {} and reason {}", statusCode, reason);
            notifyConnectionError(connectionClosedByServerException());
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("Websocket error received", error);
            notifyConnectionError(webSocketError(error));
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }

    private static RocketChatRealtimeClientException clientNotConnected() {
        return new RocketChatRealtimeClientException("Client not connected");
    }

    private static RocketChatRealtimeClientException pendingCallLimitReached() {
        return new RocketChatRealtimeClientException("Pending call limit reached");
    }

    private static RocketChatRealtimeClientException duplicateCallId() {
        return new RocketChatRealtimeClientException("Pending call with duplicate id inserted");
    }

    private static RocketChatRealtimeClientException callTimedOutException() {
        return new RocketChatRealtimeClientException("Method call timed out");
    }

    private static RocketChatRealtimeClientException unexpectedRemovedCall() {
        return new RocketChatRealtimeClientException("Unexpectedly removed pending call");
    }

    private static RocketChatRealtimeClientException connectionClosedByClientException() {
        return new RocketChatRealtimeClientException(CLIENT_CLOSING_MESSAGE);
    }

    private static RocketChatRealtimeClientException connectionClosedByServerException() {
        return new RocketChatRealtimeClientException("Connection was closed by the server");
    }

    private static RocketChatRealtimeClientException webSocketError(Throwable cause) {
        return new RocketChatRealtimeClientException("Websocket error encountered", cause);
    }
}

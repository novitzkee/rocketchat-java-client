package org.novitzkee.rocketchatclient.realtime;

import com.github.benmanes.caffeine.cache.*;
import com.squareup.moshi.Moshi;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.novitzkee.rocketchatclient.realtime.common.*;
import org.novitzkee.rocketchatclient.realtime.exception.RocketChatRealtimeClientException;
import org.novitzkee.rocketchatclient.realtime.json.CallIdAdapter;
import org.novitzkee.rocketchatclient.realtime.json.InstantAdapter;
import org.novitzkee.rocketchatclient.realtime.json.MessageTypeAdapter;
import org.novitzkee.rocketchatclient.realtime.json.MethodNameAdapter;
import org.novitzkee.rocketchatclient.realtime.message.Connect;
import org.novitzkee.rocketchatclient.realtime.message.Pong;
import org.novitzkee.rocketchatclient.realtime.util.PendingSynchronousCall;
import org.novitzkee.rocketchatclient.realtime.websocket.iface.RocketChatWebSocket;
import org.novitzkee.rocketchatclient.realtime.websocket.iface.RocketChatWebSocketListener;
import org.novitzkee.rocketchatclient.realtime.websocket.iface.RocketChatWebSocketProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
// TODO: Add call middleware.
public class RocketChatRealtimeClient {

    private static final String WEBSOCKET = ".webSocket.";

    private static final Logger WEBSOCKET_OUT_LOGGER = LoggerFactory.getLogger(RocketChatRealtimeClient.class.getName() + WEBSOCKET + "OUT");

    private static final Logger WEBSOCKET_IN_LOGGER = LoggerFactory.getLogger(RocketChatRealtimeClient.class.getName() + WEBSOCKET + "IN");

    private static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(10);

    private static final Integer DEFAULT_MAX_PENDING_CALLS = 8;

    private static final String CLIENT_CLOSING_MESSAGE = "Client closing";

    private static final Moshi MOSHI = new Moshi.Builder()
            .add(new CallIdAdapter())
            .add(new InstantAdapter())
            .add(new MessageTypeAdapter())
            .add(new MethodNameAdapter())
            .build();

    private final AtomicInteger idCounter = new AtomicInteger();

    private final Semaphore connectMutex = new Semaphore(1);

    private final Cache<@NonNull CallId, PendingSynchronousCall<?, ?>> synchronousCallsInProgress;

    private final WebSocketListener webSocketListener = new WebSocketListener();

    private final RocketChatWebSocketProvider webSocketProvider;

    @Setter(AccessLevel.PRIVATE)
    private RocketChatWebSocket webSocket;

    private volatile Exception connectionFailure;

    @Builder
    private RocketChatRealtimeClient(
            @NonNull RocketChatWebSocketProvider webSocketProvider,
            @Nullable Duration callTimeoutDuration,
            @Nullable Integer maxPendingCalls
    ) {
        this.webSocketProvider = webSocketProvider;
        this.synchronousCallsInProgress = Caffeine.newBuilder()
                .expireAfterWrite(callTimeoutDuration == null ? DEFAULT_CALL_TIMEOUT : callTimeoutDuration)
                .maximumSize(maxPendingCalls == null ? DEFAULT_MAX_PENDING_CALLS : maxPendingCalls)
                .removalListener(new PendingCallRemovalListener())
                .scheduler(Scheduler.systemScheduler())
                .build();
    }

    public boolean isConnectionActive() {
        return webSocket != null && webSocket.isConnectionActive();
    }

    public CompletableFuture<String> connect() {
        try {
            connectMutex.acquire();
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for connect mutex", e);
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }

        return doConnect().whenComplete((v, e) -> connectMutex.release());
    }

    public <T> CompletableFuture<T> performMethodCall(MethodCall<T> methodCall) {
        methodCall.id(CallId.of(idCounter.getAndIncrement()));
        return performCall(methodCall);
    }

    public CompletableFuture<Void> close() {
        log.info("Closing connection");

        notifyConnectionError(clientClosing());

        final RocketChatWebSocket ws = webSocket;
        if (ws == null) {
            return CompletableFuture.completedFuture(null);
        }

        setWebSocket(null);
        return ws.close().thenApply(ignored -> null);
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
        return webSocketProvider.createWebSocket(webSocketListener)
                .thenAccept(this::setWebSocket);
    }

    private CompletableFuture<String> establishDdpConnection() {
        return performCall(new Connect());
    }

    private <T> CompletableFuture<T> performCall(SynchronousCall<?, T> call) {
        final String outgoingMessage = MOSHI.<SynchronousCall<?, ?>>adapter(call.getClass()).toJson(call);

        final RocketChatWebSocket ws = webSocket;
        if (ws == null) {
            return CompletableFuture.failedFuture(clientNotConnected());
        }

        final PendingSynchronousCall<?, T> pendingCall = PendingSynchronousCall.start(call);

        final Exception failure = connectionFailure;
        if (failure != null) {
            pendingCall.completeExceptionally(failure);
            return pendingCall.getResult();
        }

        synchronousCallsInProgress.put(pendingCall.getId(), pendingCall);

        log.trace("Sending message: {}", outgoingMessage);
        ws.send(outgoingMessage).thenAccept(ignored -> WEBSOCKET_OUT_LOGGER.debug("{}: {}", this, outgoingMessage));

        return pendingCall.getResult();
    }

    private void receive(String message) {
        WEBSOCKET_IN_LOGGER.debug("{}: {}", this, message);

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
            webSocket.send(pong);
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

    private class WebSocketListener implements RocketChatWebSocketListener {

        @Override
        public void onMessage(String message) {
            receive(message);
        }

        @Override
        public void onClose(int statusCode, String reason) {
            log.info("Websocket connection closed with status code {} and reason {}", statusCode, reason);
            notifyConnectionError(connectionClosed());
        }

        @Override
        public void onError(Throwable error) {
            log.error("Websocket error received", error);
            notifyConnectionError(webSocketError(error));
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

    private static RocketChatRealtimeClientException clientClosing() {
        return new RocketChatRealtimeClientException(CLIENT_CLOSING_MESSAGE);
    }

    private static RocketChatRealtimeClientException connectionClosed() {
        return new RocketChatRealtimeClientException("Connection was closed");
    }

    private static RocketChatRealtimeClientException webSocketError(Throwable cause) {
        return new RocketChatRealtimeClientException("Websocket error encountered", cause);
    }
}

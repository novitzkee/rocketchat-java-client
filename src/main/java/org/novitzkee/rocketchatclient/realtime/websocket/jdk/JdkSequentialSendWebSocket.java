package org.novitzkee.rocketchatclient.realtime.websocket.jdk;

import org.novitzkee.rocketchatclient.realtime.exception.RocketChatRealtimeClientException;
import org.novitzkee.rocketchatclient.realtime.websocket.iface.RocketChatWebSocket;

import java.net.http.WebSocket;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * A {@link WebSocket} wrapper that serializes all send operations through a single daemon thread.
 *
 * <p>The JDK {@link WebSocket} contract forbids initiating a new 'send' before the previous one completes.
 * Violating this constraint throws {@link IllegalStateException} with "Send pending". This class enforces
 * the constraint by funneling every {@code sendText} and {@code sendClose} call through a single-threaded
 * executor, regardless of how many threads call into it concurrently.
 */
public class JdkSequentialSendWebSocket implements RocketChatWebSocket {

    private static final int SINGLE_THREAD = 1;

    // TODO: Make this value configurable (maybe make it consistent with max pending calls ?)
    private static final int MAX_PENDING_SENDS = 10;

    private static final Function<Object, Void> OMIT_RESULT = o -> null;

    private static final String CLIENT_CLOSING_MESSAGE = "Client closing";

    private final ExecutorService sendExecutor;

    private final WebSocket webSocket;

    JdkSequentialSendWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
        this.sendExecutor = websocketDaemonThreadExecutor();
    }

    @Override
    public boolean isConnectionActive() {
        return !webSocket.isInputClosed() && !webSocket.isOutputClosed();
    }

    @Override
    public CompletableFuture<Void> send(String message) {
        return CompletableFuture.supplyAsync(
                        () -> webSocket.sendText(message, true), sendExecutor
                )
                .thenCompose(Function.identity())
                .thenApply(OMIT_RESULT);
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.supplyAsync(
                        () -> webSocket.sendClose(WebSocket.NORMAL_CLOSURE, CLIENT_CLOSING_MESSAGE), sendExecutor
                )
                .thenCompose(Function.identity())
                .thenApply(OMIT_RESULT)
                .thenAccept(nothing -> sendExecutor.shutdown());
    }

    private static ExecutorService websocketDaemonThreadExecutor() {
        return new ThreadPoolExecutor(
                SINGLE_THREAD,
                SINGLE_THREAD,
                0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_PENDING_SENDS),
                websocketDaemonThreadFactory(),
                sendQueueFullRejectionHandler()
        );
    }


    private static ThreadFactory websocketDaemonThreadFactory() {
        return r -> {
            Thread t = new Thread(r, "websocket-send");
            t.setDaemon(true);
            return t;
        };
    }

    private static RejectedExecutionHandler sendQueueFullRejectionHandler() {
        return (r, executor) -> {
            throw sendQueueFull();
        };
    }

    private static RocketChatRealtimeClientException sendQueueFull() {
        return new RocketChatRealtimeClientException("Send queue full");
    }
}

package org.novitzkee.rocketchatclient.realtime.util;

import org.novitzkee.rocketchatclient.realtime.exception.RocketChatRealtimeClientException;

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
public class SerializingWebSocket {

    private static final int SINGLE_THREAD = 1;

    // TODO: Make this value configurable (maybe make it consistent with max pending calls ?)
    private static final int MAX_PENDING_SENDS = 1000;

    private final WebSocket delegate;

    private final ExecutorService sendExecutor;

    public SerializingWebSocket(WebSocket delegate) {
        this.delegate = delegate;
        this.sendExecutor = websocketDaemonThreadExecutor();
    }

    public CompletableFuture<WebSocket> sendText(String text, boolean last) {
        return CompletableFuture.supplyAsync(() -> delegate.sendText(text, last), sendExecutor)
                .thenCompose(Function.identity());
    }

    public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
        return CompletableFuture.supplyAsync(() -> delegate.sendClose(statusCode, reason), sendExecutor)
                .thenCompose(Function.identity());
    }

    public boolean isInputClosed() {
        return delegate.isInputClosed();
    }

    public boolean isOutputClosed() {
        return delegate.isOutputClosed();
    }

    public void shutdown() {
        sendExecutor.shutdown();
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
        return (r, executor) -> { throw sendQueueFull(); };
    }

    private static RocketChatRealtimeClientException sendQueueFull() {
        return new RocketChatRealtimeClientException("Send queue full");
    }
}

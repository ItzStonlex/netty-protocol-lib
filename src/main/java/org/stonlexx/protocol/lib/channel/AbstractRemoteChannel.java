package org.stonlexx.protocol.lib.channel;

import org.stonlexx.protocol.lib.packet.Packet;
import org.stonlexx.protocol.lib.packet.PacketProcessor;
import org.stonlexx.protocol.lib.pipeline.PacketDecoder;
import org.stonlexx.protocol.lib.pipeline.PacketEncoder;
import org.stonlexx.protocol.lib.response.FullResponseHandler;
import gnu.trove.TCollections;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.stonlexx.protocol.lib.packet.PacketProtocol;
import org.stonlexx.protocol.lib.response.ResponseHandler;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


@SuppressWarnings({"unchecked", "rawtypes"})
@RequiredArgsConstructor
public abstract class AbstractRemoteChannel implements PacketProcessor {

    public static final long DEFAULT_TIMEOUT = 5000;

    protected final SocketChannel channel;

    protected AtomicInteger requestIdCounter;
    protected TIntObjectMap<FullResponseHandler> responseHandlers;

    protected int nextRequestId() {
        if (requestIdCounter == null) {
            requestIdCounter = new AtomicInteger();
        }

        int requestId = requestIdCounter.get();

        if (requestId == Integer.MAX_VALUE) {
            requestIdCounter.set(0);
        } else {
            requestIdCounter.set(requestId + 1);
        }

        return requestId;
    }

    @Getter
    private PacketProtocol protocol;

    public void upgradeConnection(PacketProtocol protocol) {
        if (isActive()) {
            channel.pipeline().get(PacketEncoder.class).upgradeConnection(protocol);
            channel.pipeline().get(PacketDecoder.class).upgradeConnection(protocol);

            this.protocol = protocol;
        }
    }

    protected <T extends Packet> void addResponseHandler(int requestId, @NonNull FullResponseHandler<T> handler) {
        if (responseHandlers == null) {
            responseHandlers = TCollections.synchronizedMap(new TIntObjectHashMap<>());
        }

        responseHandlers.put(requestId, handler);
    }

    protected FullResponseHandler<?> removeResponseHandler(int requestId) {
        return responseHandlers == null ? null : responseHandlers.remove(requestId);
    }

    protected void handlePacket(@NonNull Packet packet) {
        if (packet.hasRequestId()) {
            FullResponseHandler handler = removeResponseHandler(packet.getRequestId());

            if (handler == null) {
                return;
            }

            handler.handleResponse(packet, null);
        }
    }

    public boolean isActive() {
        return channel.isActive();
    }

    public void close() {
        if (!isActive()) {
            return;
        }

        channel.close();
    }

    public void closeSync() {
        if (!isActive()) {
            return;
        }

        channel.close().syncUninterruptibly();
    }

    public void sendPacket(@NonNull Packet packet) {
        if (!isActive()) {
            return;
        }

        channel.writeAndFlush(packet, channel.voidPromise());
    }

    public void sendResponse(@NonNull Packet request, @NonNull Packet response) {
        response.setRequestId(request.getRequestId());

        sendPacket(response);
    }

    protected void checkChannelClosed() {
        if (!isActive()) {
            throw new IllegalStateException("Channel is closed");
        }
    }

    public <T extends Packet> T awaitPacket(@NonNull Packet packet) {
        return awaitPacket(packet, DEFAULT_TIMEOUT);
    }

    public <T extends Packet> T awaitPacket(@NonNull Packet packet, long timeout) {
        checkChannelClosed();

        AtomicReference<Throwable> refCause = new AtomicReference<>();
        CompletableFuture<T> packetFuture = new CompletableFuture<>();

        awaitPacket(packet, (response, cause) -> {
            if (cause != null) {
                refCause.set(cause);
            }

            packetFuture.complete(response == null ? null : (T) response);
        }, timeout);

        T response = packetFuture.join();

        if (refCause.get() != null) {
            throw new RuntimeException(refCause.get());
        }

        return response;
    }

    public <T extends Packet> void awaitPacket(@NonNull Packet packet,
                                               @NonNull ResponseHandler<T> handler) {
        awaitPacket(packet, (FullResponseHandler<T>) handler);
    }

    public <T extends Packet> void awaitPacket(@NonNull Packet packet,
                                               @NonNull FullResponseHandler<T> handler) {
        awaitPacket(packet, handler, DEFAULT_TIMEOUT);
    }

    public <T extends Packet> void awaitPacket(@NonNull Packet packet,
                                               @NonNull ResponseHandler<T> handler,
                                               long timeout) {
        awaitPacket(packet, (FullResponseHandler<T>) handler, timeout);
    }

    public <T extends Packet> void awaitPacket(@NonNull Packet packet,
                                               @NonNull FullResponseHandler<T> handler,
                                               long timeout) {
        checkChannelClosed();

        int requestId = nextRequestId();

        addResponseHandler(requestId, handler);

        packet.setRequestId(requestId);
        sendPacket(packet);

        channel.eventLoop().schedule(() -> {
            FullResponseHandler<?> response = removeResponseHandler(requestId);

            if (response != null) {
                response.handleResponse(null, ReadTimeoutException.INSTANCE);
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> schedule(Runnable r, long l, TimeUnit u) {
        checkChannelClosed();

        return channel.eventLoop().schedule(r, l, u);
    }

    public ScheduledFuture<?> schedule(Runnable runnable, long l, long l1, TimeUnit u) {
        checkChannelClosed();

        return channel.eventLoop().scheduleWithFixedDelay(runnable, l, l1, u);
    }

    @Override
    public void inactive() {
        if (responseHandlers != null && !responseHandlers.isEmpty()) {
            ClosedChannelException exception = new ClosedChannelException();

            responseHandlers.forEachValue(value -> {
                value.handleResponse(null, exception);

                return true;
            });

            responseHandlers.clear();
        }

        onDisconnect();
    }

    protected void onDisconnect() {
        // override me
    }


}

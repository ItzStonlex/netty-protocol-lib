package org.stonlexx.protocol.lib.channel;

import org.stonlexx.protocol.lib.packet.Packet;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.NonNull;
import org.stonlexx.protocol.lib.exception.ConnectException;
import org.stonlexx.protocol.lib.packet.PacketDirection;
import org.stonlexx.protocol.lib.packet.PacketProcessor;
import org.stonlexx.protocol.lib.response.FullResponseHandler;
import org.stonlexx.protocol.lib.response.ResponseHandler;

import javax.annotation.Nullable;
import java.nio.channels.AlreadyConnectedException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Getter
public abstract class AbstractClientChannel extends AbstractChannel {

    protected EventLoopGroup worker;

    protected Class<? extends SocketChannel> channelClass;

    protected Bootstrap bootstrap;

    private ChannelFuture future;
    private AbstractRemoteServerChannel channel;

    public AbstractClientChannel(String host, int port, int threads) {
        super(host, port, threads);

        init();
    }

    protected void checkConnectAvailability() throws ConnectException {
        if (isConnected()) {
            throw new ConnectException(this, new AlreadyConnectedException());
        }
    }

    public void connectAsynchronous() {
        connectAsynchronous(ConnectException::printStackTrace, null);
    }

    public void connectAsynchronous(@NonNull Consumer<ConnectException> errorHandler) {
        connectAsynchronous(errorHandler, null);
    }

    protected boolean reconnecting;

    public void reconnect() {
        if (reconnecting) {
            return;
        }

        reconnecting = true;
        scheduleReconnect();
    }

    protected void scheduleReconnect() {
        System.out.println("Reconnecting in 5 seconds...");
        worker.schedule(this::doReconnect, 5, TimeUnit.SECONDS);
    }

    protected void doReconnect() {
        try {
            connectSynchronized();

            reconnecting = false;
        } catch (Exception e) {
            System.out.println("Unable to reconnect: " + e);

            scheduleReconnect();
        }
    }

    public void connectAsynchronous(@Nullable Runnable success) {
        connectAsynchronous(ConnectException::printStackTrace, success);
    }

    @Override
    public PacketDirection getOutboundPacketDirection() {
        return PacketDirection.TO_SERVER;
    }

    @Override
    public PacketDirection getInboundPacketDirection() {
        return PacketDirection.TO_CLIENT;
    }


    protected abstract AbstractRemoteServerChannel newServerChannel(SocketChannel channel);

    public void connectAsynchronous(@NonNull Consumer<ConnectException> errorHandler, @Nullable Runnable success) {
        try {
            checkConnectAvailability();

            future = bootstrap.connect();

            future.addListener(future -> {
                if (future.isSuccess()) {
                    if (success != null) {
                        success.run();
                    }
                } else {
                    errorHandler.accept(new ConnectException(this, future.cause()));
                }

                this.future = null;
            });
        } catch (Exception e) {
            errorHandler.accept(new ConnectException(this, e));
        }
    }

    public void connectSynchronized() throws ConnectException {
        checkConnectAvailability();

        try {
            future = bootstrap.connect().sync();

            if (!future.isSuccess()) {
                throw future.cause();
            }
        } catch (Throwable t) {
            throw new ConnectException(this, t);
        } finally {
            future = null;
        }
    }

    protected void closeChannel() {
        channel.close();
    }

    protected void interruptFuture() {
        future.cancel(true);
        future = null;
    }

    public void closeConnection() {
        if (isConnected()) {
            closeChannel();
        } else if (future != null) {
            interruptFuture();
        }
    }

    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    public boolean isConnecting() {
        return future != null;
    }

    @Override
    protected void init() {
        super.init();

        initChannelClass();
        initEventLoopGroups();
        initBootstrap();
    }

    @Override
    public PacketProcessor newPacketProcessor(SocketChannel channel) {
        return this.channel = newServerChannel(channel);
    }

    protected void initChannelClass() {
        channelClass = epoll ? EpollSocketChannel.class : NioSocketChannel.class;
    }

    protected void initEventLoopGroups() {
        worker = epoll
                ? new EpollEventLoopGroup(threads, threadFactory)
                : new NioEventLoopGroup(threads, threadFactory);
    }

    protected void initBootstrap() {
        bootstrap = new Bootstrap()
                .option(ChannelOption.TCP_NODELAY, true)
                .remoteAddress(socketAddress)
                .channel(channelClass)
                .handler(channelInitializer)
                .group(worker);
    }

    public void sendPacket(Packet packet) {
        if (isConnected()) {
            channel.sendPacket(packet);
        }
    }


    public <T extends Packet> T awaitPacket(@NonNull Packet packet) {
        return channel.awaitPacket(packet);
    }

    public <T extends Packet> void awaitPacket(@NonNull Packet packet, @NonNull FullResponseHandler<T> handler) {
        channel.awaitPacket(packet, handler);
    }

    public <T extends Packet> T awaitPacket(@NonNull Packet packet, long timeout) {
        return channel.awaitPacket(packet, timeout);
    }

    public <T extends Packet> void awaitPacket(@NonNull Packet packet, @NonNull FullResponseHandler<T> handler, long timeout) {
        channel.awaitPacket(packet, handler, timeout);
    }

    public <T extends Packet> void awaitPacket(@NonNull Packet packet, @NonNull ResponseHandler<T> handler) {
        channel.awaitPacket(packet, handler);
    }

    public <T extends Packet> void awaitPacket(@NonNull Packet packet, @NonNull ResponseHandler<T> handler, long timeout) {
        channel.awaitPacket(packet, handler, timeout);
    }

}

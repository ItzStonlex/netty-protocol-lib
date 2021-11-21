package org.stonlexx.protocol.lib.channel;

import org.stonlexx.protocol.lib.packet.PacketProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import lombok.NonNull;
import org.stonlexx.protocol.lib.exception.BindException;
import org.stonlexx.protocol.lib.packet.PacketDirection;

import javax.annotation.Nullable;
import java.nio.channels.AlreadyBoundException;
import java.util.function.Consumer;

@Getter
public abstract class AbstractServerChannel extends AbstractChannel implements PacketProcessor {

    protected EventLoopGroup boss;
    protected EventLoopGroup worker;

    protected ChannelFactory<ServerSocketChannel> channelFactory;

    protected ServerBootstrap bootstrap;

    private ChannelFuture future;
    private ServerSocketChannel channel;

    public AbstractServerChannel(String host, int port, int threads) {
        super(host, port, threads);

        init();
    }

    protected void checkBindAvailability() throws BindException {
        if (isConnected()) {
            throw new BindException(this, new AlreadyBoundException());
        }
    }

    public void bindAsynchronous() {
        bindAsynchronous(BindException::printStackTrace, null);
    }

    public void bindAsynchronous(@NonNull Consumer<BindException> errorHandler) {
        bindAsynchronous(errorHandler, null);
    }

    public void bindAsynchronous(@Nullable Runnable success) {
        bindAsynchronous(BindException::printStackTrace, success);
    }

    public void bindAsynchronous(@NonNull Consumer<BindException> errorHandler, @Nullable Runnable success) {
        try {
            checkBindAvailability();

            future = bootstrap.bind();

            future.addListener(future -> {
                if (future.isSuccess()) {
                    channel = (ServerSocketChannel) this.future.channel();

                    if (success != null) {
                        success.run();
                    }
                } else {
                    errorHandler.accept(new BindException(this, future.cause()));
                }

                this.future = null;
            });
        } catch (Exception e) {
            errorHandler.accept(new BindException(this, e));
        }
    }

    public void bindSynchronized() throws BindException {
        checkBindAvailability();

        try {
            future = bootstrap.bind().sync();

            if (future.isSuccess()) {
                channel = (ServerSocketChannel) future.channel();
            } else {
                throw future.cause();
            }
        } catch (Throwable t) {
            throw new BindException(this, t);
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

    protected abstract AbstractRemoteClientChannel newClientChannel(SocketChannel channel);

    @Override
    public PacketProcessor newPacketProcessor(SocketChannel channel) {
        return newClientChannel(channel);
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

    @Override
    protected void init() {
        super.init();

        initChannelFactory();
        initEventLoopGroups();
        initBootstrap();
    }

    protected void initChannelFactory() {
        channelFactory = epoll ? EpollServerSocketChannel::new : NioServerSocketChannel::new;
    }

    @Override
    public PacketDirection getOutboundPacketDirection() {
        return PacketDirection.TO_CLIENT;
    }

    @Override
    public PacketDirection getInboundPacketDirection() {
        return PacketDirection.TO_SERVER;
    }

    protected void initEventLoopGroups() {
        if (epoll) {
            boss = new EpollEventLoopGroup(1, threadFactory);
            worker = new EpollEventLoopGroup(threadFactory);
        } else {
            boss = new NioEventLoopGroup(1, threadFactory);
            worker = new NioEventLoopGroup(threadFactory);
        }
    }

    protected void initBootstrap() {
        bootstrap = new ServerBootstrap()
                .childOption(ChannelOption.TCP_NODELAY, true)
                .localAddress(socketAddress)
                .channelFactory(channelFactory)
                .childHandler(channelInitializer)
                .group(boss, worker);
    }

}

package org.stonlexx.protocol.lib.channel;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.socket.SocketChannel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.stonlexx.protocol.lib.packet.PacketDirection;
import org.stonlexx.protocol.lib.packet.PacketProcessor;
import org.stonlexx.protocol.lib.pipeline.Pipeline;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;

@Getter
@RequiredArgsConstructor
public abstract class AbstractChannel {

    protected static final boolean epoll = Epoll.isAvailable();

    protected static final ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("[Netty] EventLoopGroup #%s")
            .build();

    protected ChannelInitializer<SocketChannel> channelInitializer;

    protected final InetSocketAddress socketAddress;
    protected final int threads;

    public AbstractChannel(String host, int port, int threads) {
        this(new InetSocketAddress(host, port), threads);
    }

    public String getAddress() {
        return socketAddress.getHostName();
    }

    public int getPort() {
        return socketAddress.getPort();
    }

    public abstract PacketProcessor newPacketProcessor(SocketChannel channel);

    public abstract PacketDirection getOutboundPacketDirection();
    public abstract PacketDirection getInboundPacketDirection();

    public boolean isEpoll() {
        return epoll;
    }

    protected void initPipeline(SocketChannel channel) {
        Pipeline.initPipeline(this, channel);
    }

    protected void initChannelInitializer() {
        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                initPipeline(ch);
            }
        };
    }

    protected void init() {
        initChannelInitializer();
    }

}

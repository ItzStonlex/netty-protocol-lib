package org.stonlexx.protocol.lib.pipeline;

import org.stonlexx.protocol.lib.channel.AbstractChannel;
import org.stonlexx.protocol.lib.metrics.PerformanceMetrics;
import org.stonlexx.protocol.lib.metrics.TrafficCounter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.util.function.BiConsumer;

public final class Pipeline {

    public static final String FRAMER = "packet-framer";
    public static final String ENCODER = "packet-encoder";
    public static final String DECODER = "packet-decoder";
    public static final String HANDLER = "packet-handler";

    private static BiConsumer<AbstractChannel, SocketChannel> pipelineInitializer;

    public static void initPipeline(AbstractChannel channel, SocketChannel socket) {
        if (pipelineInitializer != null) {
            pipelineInitializer.accept(channel, socket);
        }
    }

    public static void addCustomInitializer(BiConsumer<AbstractChannel, SocketChannel> initializer) {
        if (pipelineInitializer == null) {
            pipelineInitializer = initializer;
        } else {
            pipelineInitializer = pipelineInitializer.andThen(initializer);
        }
    }

    static {
        addCustomInitializer((channel, socket) -> {
            ChannelPipeline pipeline = socket.pipeline();

            pipeline.addLast(FRAMER, new PacketFramer());
            pipeline.addLast(ENCODER, new PacketEncoder(channel.getOutboundPacketDirection()));
            pipeline.addLast(DECODER, new PacketDecoder(channel.getInboundPacketDirection()));
            pipeline.addLast(HANDLER, new PacketHandler(channel.newPacketProcessor(socket)));

            if (PerformanceMetrics.isMetricsEnabled()) {
                pipeline.addFirst(new TrafficCounter());
            }
        });
    }

}

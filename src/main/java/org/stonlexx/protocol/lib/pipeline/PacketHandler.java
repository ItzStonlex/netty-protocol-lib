package org.stonlexx.protocol.lib.pipeline;

import org.stonlexx.protocol.lib.packet.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import org.stonlexx.protocol.lib.packet.PacketProcessor;

@RequiredArgsConstructor
public class PacketHandler extends ChannelInboundHandlerAdapter {

    private final PacketProcessor processor;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        processor.active();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        processor.inactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Packet) {
            Packet packet = (Packet) msg;

            try {
                processor.process(packet);
            } catch (Packet response) {
                response.setRequestId(packet.getRequestId());

                ctx.writeAndFlush(response);
            }

            return;
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        processor.process(cause);
    }
}
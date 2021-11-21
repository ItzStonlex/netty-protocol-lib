package org.stonlexx.protocol.lib.metrics;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

public class TrafficCounter extends ChannelHandlerAdapter {

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().addFirst("Download", new Download());
        ctx.pipeline().addFirst("Upload", new Upload());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().remove(Download.class);
        ctx.pipeline().remove(Upload.class);
    }

    public static class Upload extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof ByteBuf) {
                PerformanceMetrics.TRAFFIC_UPLOAD.addValue(((ByteBuf) msg).readableBytes());
            }

            super.write(ctx, msg, promise);
        }
    }

    public static class Download extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof ByteBuf) {
                PerformanceMetrics.TRAFFIC_DOWNLOAD.addValue(((ByteBuf) msg).readableBytes());
            }

            super.channelRead(ctx, msg);
        }
    }

}

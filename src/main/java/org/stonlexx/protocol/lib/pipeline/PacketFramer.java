package org.stonlexx.protocol.lib.pipeline;

import org.stonlexx.protocol.lib.util.PacketUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

public class PacketFramer extends ByteToMessageCodec<ByteBuf> {

    private static boolean DIRECT_WARNING = false;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf from, ByteBuf to) {
        int packetSize = from.readableBytes();
        int headerSize = PacketUtils.getVarIntSize(packetSize);

        to.ensureWritable(packetSize + headerSize);

        PacketUtils.writeVarInt(to, packetSize);
        to.writeBytes(from, from.readerIndex(), packetSize);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        buf.markReaderIndex();

        byte[] header = new byte[3];

        for (int i = 0; i < header.length; ++i) {
            if (!buf.isReadable()) {
                buf.resetReaderIndex();
                return;
            }

            header[i] = buf.readByte();

            if (header[i] >= 0) {
                ByteBuf headerBuf = Unpooled.wrappedBuffer(header);

                try {
                    int length = PacketUtils.readVarInt(headerBuf);

                    if (buf.readableBytes() < length) {
                        buf.resetReaderIndex();
                        return;
                    }

                    if (buf.hasMemoryAddress()) {
                        out.add(buf.slice(buf.readerIndex(), length).retain());
                        buf.skipBytes(length);
                    } else {
                        if (!DIRECT_WARNING) {
                            DIRECT_WARNING = true;
                            System.err.println("Netty is not using direct IO buffers.");
                        }

                        ByteBuf dst = ctx.alloc().directBuffer(length);
                        buf.readBytes(dst);
                        out.add(dst);
                    }
                } finally {
                    headerBuf.release();
                }

                return;
            }
        }

        throw new CorruptedFrameException("length wider than 21-bit");
    }
}
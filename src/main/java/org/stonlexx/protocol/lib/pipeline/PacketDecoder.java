package org.stonlexx.protocol.lib.pipeline;

import org.stonlexx.protocol.lib.metrics.PerformanceMetrics;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import lombok.RequiredArgsConstructor;
import org.stonlexx.protocol.lib.packet.Packet;
import org.stonlexx.protocol.lib.packet.PacketDirection;
import org.stonlexx.protocol.lib.packet.PacketProtocol;
import org.stonlexx.protocol.lib.util.PacketUtils;

import java.util.List;

@RequiredArgsConstructor
public class PacketDecoder extends ByteToMessageDecoder {

    private final PacketDirection direction;
    private PacketProtocol state = PacketProtocol.HANDSHAKE;

    public void upgradeConnection(PacketProtocol newState) {
        state = newState;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            if (in.readableBytes() == 0) {
                return;
            }

            int id = PacketUtils.readVarInt(in);

            Packet packet = direction.getMapper(state).newPacket(id);

            if (packet == null) {
                callDecoderException("Bad Packet: [ID: %s, State: %s, Direction: %s]",
                        null, id, state, direction);
            }

            try {
                packet.readPacket(in);
            } catch (Exception e) {
                callDecoderException("Unable to decode packet: %s", e, packet);
            }

            if (in.readableBytes() > 0) {
                callDecoderException("Packet %s read, but %s bytes left", null, packet, in.readableBytes());
            }

            out.add(packet);

            PerformanceMetrics.RECEIVED_PACKETS.addValue(1);
        } finally {
            in.skipBytes(in.readableBytes());
        }
    }

    private void callDecoderException(String message, Throwable cause, Object... params) {
        throw new DecoderException(String.format(message, params), cause);
    }

}
package org.stonlexx.protocol.lib.pipeline;

import org.stonlexx.protocol.lib.metrics.PerformanceMetrics;
import org.stonlexx.protocol.lib.packet.Packet;
import org.stonlexx.protocol.lib.util.PacketUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.RequiredArgsConstructor;
import org.stonlexx.protocol.lib.packet.PacketDirection;
import org.stonlexx.protocol.lib.packet.PacketProtocol;

@RequiredArgsConstructor
public class PacketEncoder extends MessageToByteEncoder<Packet> {

    private final PacketDirection direction;
    private PacketProtocol state = PacketProtocol.HANDSHAKE;

    public void upgradeConnection(PacketProtocol newState) {
        state = newState;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf buf) throws Exception {
        int packetId = direction.getMapper(state).getPacketId(packet.getClass());

        if (packetId == -1) {
            throw new EncoderException("Tried to send unregistered packet: [Packet: " + packet + ", State: " + state + "]");
        }

        PacketUtils.writeVarInt(buf, packetId);
        packet.writePacket(buf);

        PerformanceMetrics.SENT_PACKETS.addValue(1);
    }
}
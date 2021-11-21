package org.stonlexx.protocol.lib.channel;

import io.netty.channel.socket.SocketChannel;
import lombok.NonNull;
import org.stonlexx.protocol.lib.packet.Packet;

public abstract class AbstractRemoteClientChannel extends AbstractRemoteChannel {

    public AbstractRemoteClientChannel(SocketChannel channel) {
        super(channel);
    }

    @Override
    public void process(@NonNull Packet packet) throws Exception {
        packet.process(this);

        handlePacket(packet);
    }

}

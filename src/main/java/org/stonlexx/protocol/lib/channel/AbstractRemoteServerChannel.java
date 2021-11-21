package org.stonlexx.protocol.lib.channel;

import io.netty.channel.socket.SocketChannel;
import lombok.NonNull;
import org.stonlexx.protocol.lib.packet.Packet;

public abstract class AbstractRemoteServerChannel extends AbstractRemoteChannel {

    protected final AbstractClientChannel client;

    public AbstractRemoteServerChannel(AbstractClientChannel client, SocketChannel channel) {
        super(channel);

        this.client = client;
    }

    @Override
    public void process(@NonNull Packet packet) throws Exception {
        packet.process(this);

        handlePacket(packet);
    }

    @Override
    public void inactive() {
        client.reconnect();
    }
}

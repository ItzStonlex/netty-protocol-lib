import org.stonlexx.protocol.lib.channel.AbstractClientChannel;
import org.stonlexx.protocol.lib.channel.AbstractRemoteServerChannel;
import org.stonlexx.protocol.lib.metrics.PerformanceMetrics;
import org.stonlexx.protocol.lib.packet.PacketProtocol;
import io.netty.channel.socket.SocketChannel;
import packet.SPacket;
import serialize.Player;

public final class Client extends AbstractClientChannel {

    public static void main(String[] args) {
        PerformanceMetrics.startMetrics();

        Player player = new Player("stonlex", 1);
        Client client = new Client();

        client.connectAsynchronous((e) -> client.reconnect(), () -> {

            System.out.println("SUCCESS: Channel was success connected to " + client.socketAddress);

            client.sendPacket(new SPacket(player));
        });
    }


    public Client() {
        super("127.0.0.1", 1010, 2);

        PacketProtocol.HANDSHAKE.TO_SERVER.registerPacket(0x00, SPacket.class);
        PacketProtocol.HANDSHAKE.TO_CLIENT.registerPacket(0x00, SPacket.class);
    }

    @Override
    protected AbstractRemoteServerChannel newServerChannel(SocketChannel channel) {
        return new ServerChannelHandler(this, channel);
    }

    private static class ServerChannelHandler extends AbstractRemoteServerChannel {

        public ServerChannelHandler(AbstractClientChannel client, SocketChannel channel) {
            super(client, channel);
        }

    }

}

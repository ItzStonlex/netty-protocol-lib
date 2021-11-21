import org.stonlexx.protocol.lib.channel.AbstractRemoteClientChannel;
import org.stonlexx.protocol.lib.channel.AbstractServerChannel;
import org.stonlexx.protocol.lib.metrics.PerformanceMetrics;
import org.stonlexx.protocol.lib.packet.PacketProtocol;
import io.netty.channel.socket.SocketChannel;
import packet.SPacket;

public final class Server extends AbstractServerChannel {

    public static void main(String[] args) {
        PerformanceMetrics.startMetrics();

        Server server = new Server();
        server.bindAsynchronous(() -> System.out.println("SUCCESS: Channel was success bind on " + server.getChannel().localAddress()));
    }


    public Server() {
        super("127.0.0.1", 1010, 2);

        PacketProtocol.HANDSHAKE.TO_SERVER.registerPacket(0x00, SPacket.class);
        PacketProtocol.HANDSHAKE.TO_CLIENT.registerPacket(0x00, SPacket.class);
    }

    @Override
    protected AbstractRemoteClientChannel newClientChannel(SocketChannel channel) {
        return new ClientChannelHandler(channel);
    }


    private static class ClientChannelHandler extends AbstractRemoteClientChannel {

        public ClientChannelHandler(SocketChannel channel) {
            super(channel);
        }
    }

}
